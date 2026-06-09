package com.aiplatform.assistant.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HTTP 请求工具:支持 GET/POST/PUT/DELETE,带 timeout。
 */
@Slf4j
@Component
public class HttpTool implements AssistantTool {

    @Override
    public String name() {
        return "http_request";
    }

    @Override
    public String displayName() {
        return "HTTP 请求";
    }

    @Override
    public String description() {
        return "发送 HTTP 请求到指定 URL,支持 GET/POST/PUT/DELETE,返回响应内容(截断 2000 字符)。";
    }

    @Override
    public String category() {
        return "工具";
    }

    @Override
    public String parametersSchema() {
        return "{\"url\":\"string,required\",\"method\":\"string,default:GET,options:GET,POST,PUT,DELETE\","
                + "\"headers\":\"object,optional\",\"body\":\"string,optional\","
                + "\"timeout\":\"number,default:10000\"}";
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext context) {
        String url = (String) args.get("url");
        if (url == null || url.isBlank()) return ToolResult.fail("url 不能为空");
        String method = args.get("method") == null ? "GET" : args.get("method").toString().toUpperCase();
        Object headers = args.get("headers");
        Object body = args.get("body");
        int timeout = args.get("timeout") == null ? 10000 : ((Number) args.get("timeout")).intValue();

        try {
            HttpRequest req = HttpUtil.createRequest(Method.valueOf(method), url).timeout(timeout);
            if (headers instanceof Map<?, ?> hm) {
                hm.forEach((k, v) -> {
                    String key = String.valueOf(k);
                    if (v instanceof List<?> list && !list.isEmpty()) {
                        req.header(key, String.valueOf(list.get(0)));
                    } else {
                        req.header(key, String.valueOf(v));
                    }
                });
            }
            if (body != null && (method.equals("POST") || method.equals("PUT"))) {
                req.body(String.valueOf(body));
            }
            try (HttpResponse resp = req.execute()) {
                String text = resp.body();
                if (text == null) text = "";
                if (text.length() > 2000) text = text.substring(0, 2000) + "...(truncated)";
                String summary = String.format("HTTP %d (%d bytes)", resp.getStatus(), text.length());
                String detail = String.format("{\"status\":%d,\"headers\":%s,\"body\":%s}",
                        resp.getStatus(),
                        headersAsJson(resp.headers()), JsonSnippet(text));
                return ToolResult.ok(summary, detail);
            }
        } catch (Exception e) {
            log.warn("HttpTool error: url={}, method={}", url, method, e);
            return ToolResult.fail("HTTP 调用失败: " + e.getMessage());
        }
    }

    private String JsonSnippet(String s) {
        if (s == null) return "null";
        String esc = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        if (esc.length() > 500) esc = esc.substring(0, 500) + "...";
        return "\"" + esc + "\"";
    }

    private String headersAsJson(Map<String, List<String>> headers) {
        if (headers == null || headers.isEmpty()) return "{}";
        return headers.entrySet().stream()
                .map(e -> "\"" + e.getKey() + "\":\"" + (e.getValue() == null ? "" : e.getValue().stream()
                        .map(v -> v == null ? "" : v.replace("\"", "\\\""))
                        .collect(Collectors.joining(","))) + "\"")
                .collect(Collectors.joining(",", "{", "}"));
    }
}
