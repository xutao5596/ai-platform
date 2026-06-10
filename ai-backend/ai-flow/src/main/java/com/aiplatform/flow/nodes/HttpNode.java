package com.aiplatform.flow.nodes;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.aiplatform.flow.spi.FlowNode;
import com.aiplatform.flow.spi.NodeContext;
import com.aiplatform.flow.spi.NodeExecuteResult;
import com.aiplatform.flow.spi.NodeSchema;
import com.aiplatform.flow.spi.Property;
import com.aiplatform.framework.observability.BusinessMetrics;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP 请求节点:发起 HTTP 调用,返回响应。
 *
 * 配置:
 *  - method: GET/POST/PUT/DELETE
 *  - url: 请求 URL
 *  - headers: 请求头 JSON
 *  - body: 请求体(支持 {{var}})
 *  - bodyType: json / form / text
 *  - timeoutMs: 超时 ms
 *  - outputKey: 输出变量 Key(默认 "httpResponse")
 */
@Slf4j
@LiteflowComponent("http")
@Component
public class HttpNode extends NodeComponent implements FlowNode {

    @Override
    public String getTypeKey() {
        return "http";
    }

    @Override
    public String getDisplayName() {
        return "HTTP 请求";
    }

    @Override
    public String getCategory() {
        return "tool";
    }

    @Override
    public String getIcon() {
        return "mdi:web";
    }

    @Override
    public String getColor() {
        return "#17A2B8";
    }

    @Override
    public String getDescription() {
        return "发起 HTTP 请求并返回响应。";
    }

    @Override
    public NodeSchema getSchema() {
        List<Property.Option> methods = new ArrayList<>();
        methods.add(new Property.Option("GET", "GET"));
        methods.add(new Property.Option("POST", "POST"));
        methods.add(new Property.Option("PUT", "PUT"));
        methods.add(new Property.Option("DELETE", "DELETE"));
        methods.add(new Property.Option("PATCH", "PATCH"));
        return NodeSchema.of(
                List.of(
                        new Property("method", "方法", "select", true, "GET", "HTTP 方法", methods),
                        new Property("url", "URL", "string", true, "", "请求 URL(支持 {{var}})", null),
                        new Property("headers", "请求头", "json", false, "{}", "JSON 对象,key-value", null),
                        new Property("body", "请求体", "textarea", false, "", "请求体(支持 {{var}})", null),
                        new Property("bodyType", "请求体类型", "select", false, "text", "json/form/text", null),
                        new Property("timeoutMs", "超时 (ms)", "number", false, 30000, "请求超时(毫秒)", null),
                        new Property("outputKey", "输出变量 Key", "string", false, "httpResponse", "响应写入 variables 的 key", null)
                ),
                List.of(
                        new Property("status", "HTTP 状态码", "number", false, null, "响应状态码", null),
                        new Property("body", "响应内容", "string", false, null, "响应体文本", null)
                )
        );
    }

    @Override
    public NodeExecuteResult execute(NodeContext ctx) {
        String method = ctx.getConfigString("method");
        if (method == null) method = "GET";
        method = method.toUpperCase();
        String url = renderUrl(ctx.getConfigString("url"), ctx);
        if (url == null || url.isBlank()) {
            return NodeExecuteResult.fail("HTTP 节点缺少 url");
        }
        int timeout = readInt(ctx, "timeoutMs", 30000);
        String bodyType = ctx.getConfigString("bodyType");
        if (bodyType == null) bodyType = "text";
        String bodyRaw = renderBody(ctx.getConfigString("body"), ctx);
        String headersJson = ctx.getConfigString("headers");

        try {
            Method m = Method.valueOf(method);
            HttpRequest req = HttpUtil.createRequest(m, url)
                    .timeout(timeout);

            if (headersJson != null && !headersJson.isBlank()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> headers = com.aiplatform.common.util.JsonUtils.fromJson(headersJson, Map.class);
                    if (headers != null) {
                        for (Map.Entry<String, Object> e : headers.entrySet()) {
                            req.header(e.getKey(), String.valueOf(e.getValue()));
                        }
                    }
                } catch (Exception ignore) {
                }
            }

            if (bodyRaw != null && !bodyRaw.isBlank()) {
                if ("json".equalsIgnoreCase(bodyType)) {
                    req.body(bodyRaw).contentType("application/json");
                } else if ("form".equalsIgnoreCase(bodyType)) {
                    req.form(bodyRaw).contentType("application/x-www-form-urlencoded");
                } else {
                    req.body(bodyRaw);
                }
            }

            try (HttpResponse resp = req.execute()) {
                String respBody = resp.body();
                int status = resp.getStatus();
                Map<String, Object> out = new HashMap<>();
                out.put("status", status);
                out.put("body", respBody);
                String outputKey = ctx.getConfigString("outputKey");
                if (outputKey == null || outputKey.isBlank()) outputKey = "httpResponse";
                out.put(outputKey, Map.of("status", status, "body", respBody));
                if (status >= 400) {
                    BusinessMetrics.flowNodeExecute("http", "failed");
                    return NodeExecuteResult.fail("HTTP " + status + ": " + truncate(respBody, 500));
                }
                BusinessMetrics.flowNodeExecute("http", "success");
                return NodeExecuteResult.success(out);
            }
        } catch (Exception e) {
            log.warn("HTTP 请求失败: url={}", url, e);
            BusinessMetrics.flowNodeExecute("http", "failed");
            return NodeExecuteResult.fail("HTTP 请求失败: " + e.getMessage());
        }
    }

    @Override
    public void process() throws Exception {
        NodeContext ctx = this.getContextBean(NodeContext.class);
        if (ctx != null) {
            var spec = ctx.getSpec(this.getNodeId());
            if (spec != null) ctx.setNodeConfig(spec.config);
        }
        if (ctx == null) {
            log.warn("HttpNode 收到空 NodeContext,跳过");
            return;
        }
        NodeExecuteResult r = execute(ctx);
        if (r != null && !r.isSuccess()) {
            throw new RuntimeException(r.getErrorMsg() == null ? "Node execution failed" : r.getErrorMsg());
        }
    }

    private String renderUrl(String url, NodeContext ctx) {
        if (url == null) return null;
        return render(url, ctx);
    }

    private String renderBody(String body, NodeContext ctx) {
        if (body == null) return null;
        return render(body, ctx);
    }

    private String render(String s, NodeContext ctx) {
        if (s == null) return null;
        String r = s;
        if (ctx.getVariables() != null) {
            for (Map.Entry<String, Object> e : ctx.getVariables().entrySet()) {
                String token = "{{" + e.getKey() + "}}";
                if (r.contains(token)) {
                    r = r.replace(token, e.getValue() == null ? "" : String.valueOf(e.getValue()));
                }
            }
        }
        if (ctx.getInput() != null) {
            for (Map.Entry<String, Object> e : ctx.getInput().entrySet()) {
                String token = "{{input." + e.getKey() + "}}";
                if (r.contains(token)) {
                    r = r.replace(token, e.getValue() == null ? "" : String.valueOf(e.getValue()));
                }
            }
        }
        return r;
    }

    private Integer readInt(NodeContext ctx, String key, int def) {
        Object v = ctx.getConfig(key);
        if (v == null) return def;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() > n ? s.substring(0, n) + "..." : s;
    }
}
