package com.aiplatform.flow.nodes;

import com.aiplatform.flow.spi.FlowNode;
import com.aiplatform.flow.spi.NodeContext;
import com.aiplatform.flow.spi.NodeExecuteResult;
import com.aiplatform.flow.spi.NodeSchema;
import com.aiplatform.flow.spi.Property;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 变量赋值节点:对 variables 进行赋值。
 *
 * 配置:
 *  - assignments: JSON 数组 [{key, value, valueType}]
 *      key: 变量名
 *      value: 值(支持 {{var}})
 *      valueType: string / number / boolean / json
 */
@Slf4j
@LiteflowComponent("set_var")
@Component
public class SetVarNode extends NodeComponent implements FlowNode {

    @Override
    public String getTypeKey() {
        return "set_var";
    }

    @Override
    public String getDisplayName() {
        return "变量赋值";
    }

    @Override
    public String getCategory() {
        return "data";
    }

    @Override
    public String getIcon() {
        return "mdi:variable";
    }

    @Override
    public String getColor() {
        return "#6366F1";
    }

    @Override
    public String getDescription() {
        return "对 variables 进行赋值。支持 {{var}} 插值。";
    }

    @Override
    public NodeSchema getSchema() {
        return NodeSchema.of(
                List.of(
                        new Property("assignments", "赋值列表", "json", false,
                                "[{\"key\":\"myVar\",\"value\":\"hello {{name}}\",\"valueType\":\"string\"}]",
                                "JSON 数组,每个元素 {key, value, valueType}", null)
                ),
                List.of(
                        new Property("assigned", "已赋值", "json", false, null, "赋值详情", null)
                )
        );
    }

    @Override
    public NodeExecuteResult execute(NodeContext ctx) {
        String assignmentsJson = ctx.getConfigString("assignments");
        if (assignmentsJson == null || assignmentsJson.isBlank()) {
            return NodeExecuteResult.success(Map.of("assigned", new ArrayList<>()));
        }
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> list = com.aiplatform.common.util.JsonUtils.fromJson(assignmentsJson, List.class);
            if (list == null) {
                return NodeExecuteResult.success(Map.of("assigned", new ArrayList<>()));
            }
            List<Map<String, Object>> assigned = new ArrayList<>();
            for (Map<String, Object> item : list) {
                String key = (String) item.get("key");
                if (key == null || key.isBlank()) continue;
                Object rawValue = item.get("value");
                String type = (String) item.get("valueType");
                if (type == null) type = "string";
                Object finalValue = coerce(render(rawValue, ctx), type);
                ctx.putVariable(key, finalValue);
                Map<String, Object> rec = new HashMap<>();
                rec.put("key", key);
                rec.put("value", finalValue);
                assigned.add(rec);
            }
            return NodeExecuteResult.success(Map.of("assigned", assigned));
        } catch (Exception e) {
            log.warn("SetVar 解析失败: {}", e.getMessage());
            return NodeExecuteResult.fail("变量赋值失败: " + e.getMessage());
        }
    }

    @Override
    public void process() throws Exception {
        NodeContext ctx = this.getContextBean(NodeContext.class);
        if (ctx == null) {
            log.warn("SetVarNode 收到空 NodeContext,跳过");
            return;
        }
        execute(ctx);
    }

    private String render(Object v, NodeContext ctx) {
        if (v == null) return null;
        String s = String.valueOf(v);
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

    private Object coerce(String s, String type) {
        if (s == null) return null;
        return switch (type.toLowerCase()) {
            case "number" -> {
                try {
                    yield Double.parseDouble(s);
                } catch (NumberFormatException e) {
                    yield s;
                }
            }
            case "boolean" -> Boolean.parseBoolean(s);
            case "json" -> {
                try {
                    yield com.aiplatform.common.util.JsonUtils.fromJson(s, Object.class);
                } catch (Exception e) {
                    yield s;
                }
            }
            default -> s;
        };
    }
}
