package com.aiplatform.flow.nodes;

import com.aiplatform.ai.llm.ChatService;
import com.aiplatform.flow.spi.FlowNode;
import com.aiplatform.flow.spi.NodeContext;
import com.aiplatform.flow.spi.NodeExecuteResult;
import com.aiplatform.flow.spi.NodeSchema;
import com.aiplatform.flow.spi.Property;
import com.aiplatform.framework.observability.BusinessMetrics;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM 节点:调用 ChatService 与 LLM 交互。
 *
 * 配置:
 *  - modelId: 模型 ID(必填)
 *  - systemPrompt: 系统提示词(可选)
 *  - userMessageTemplate: 用户消息模板,支持 {{var}} 占位符
 *  - temperature: 温度
 *  - maxTokens: 最大输出
 *  - outputKey: 输出变量 Key(默认 "llmOutput")
 */
@Slf4j
@LiteflowComponent("llm")
@Component
@RequiredArgsConstructor
public class LlmNode extends NodeComponent implements FlowNode {

    private final ChatService chatService;

    @Override
    public String getTypeKey() {
        return "llm";
    }

    @Override
    public String getDisplayName() {
        return "LLM 调用";
    }

    @Override
    public String getCategory() {
        return "ai";
    }

    @Override
    public String getIcon() {
        return "mdi:robot";
    }

    @Override
    public String getColor() {
        return "#409EFF";
    }

    @Override
    public String getDescription() {
        return "调用大语言模型生成回复。支持变量插值 {{varName}}。";
    }

    @Override
    public NodeSchema getSchema() {
        return NodeSchema.of(
                List.of(
                        new Property("modelId", "模型", "model", true,
                                null, "选择模型", null),
                        new Property("systemPrompt", "系统提示词", "textarea", false,
                                "你是一个有帮助的助手。", "指导 LLM 的行为", null),
                        new Property("userMessage", "用户消息模板", "textarea", true,
                                "{{query}}", "支持 {{var}} 占位符,变量从 ctx.variables 取", null),
                        new Property("temperature", "温度", "number", false,
                                0.7, "0-2 之间的浮点数", null),
                        new Property("maxTokens", "最大输出", "number", false,
                                2048, "最大 token 数", null),
                        new Property("outputKey", "输出变量 Key", "string", false,
                                "llmOutput", "把 LLM 输出写入到 variables 的 key", null)
                ),
                List.of(
                        new Property("content", "LLM 回复", "string", false, null, "LLM 生成的文本", null),
                        new Property("inputTokens", "输入 token", "number", false, null, "消耗的输入 token", null),
                        new Property("outputTokens", "输出 token", "number", false, null, "消耗的输出 token", null)
                )
        );
    }

    @Override
    public NodeExecuteResult execute(NodeContext ctx) {
        Long modelId = ctx.getConfigLong("modelId");
        if (modelId == null) return NodeExecuteResult.fail("LLM 节点缺少 modelId");
        String systemPrompt = ctx.getConfigString("systemPrompt");
        String userTemplate = ctx.getConfigString("userMessage");
        Double temperature = readDouble(ctx, "temperature", 0.7);
        Integer maxTokens = readInt(ctx, "maxTokens", 2048);
        String outputKey = ctx.getConfigString("outputKey");
        if (outputKey == null || outputKey.isBlank()) outputKey = "llmOutput";

        String userMessage = render(userTemplate, ctx);

        try {
            ChatService.ChatResult result = chatService.chat(modelId, systemPrompt, userMessage, temperature, maxTokens);
            Map<String, Object> out = new HashMap<>();
            out.put("content", result.content());
            out.put("inputTokens", result.inputTokens());
            out.put("outputTokens", result.outputTokens());
            out.put(outputKey, result.content());
            BusinessMetrics.flowNodeExecute("llm", "success");
            return NodeExecuteResult.success(out);
        } catch (Exception e) {
            log.error("LLM 节点执行失败: modelId={}, error={}", modelId, e.getMessage());
            BusinessMetrics.flowNodeExecute("llm", "failed");
            return NodeExecuteResult.fail("LLM 调用失败: " + e.getMessage());
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
            log.warn("LlmNode 收到空 NodeContext,跳过");
            return;
        }
        NodeExecuteResult r = execute(ctx);
        if (r != null && !r.isSuccess()) {
            throw new RuntimeException(r.getErrorMsg() == null ? "Node execution failed" : r.getErrorMsg());
        }
    }

    private String render(String template, NodeContext ctx) {
        if (template == null) return "";
        String result = template;
        if (ctx.getVariables() != null) {
            for (Map.Entry<String, Object> e : ctx.getVariables().entrySet()) {
                String token = "{{" + e.getKey() + "}}";
                if (result.contains(token)) {
                    result = result.replace(token, e.getValue() == null ? "" : String.valueOf(e.getValue()));
                }
            }
        }
        if (ctx.getInput() != null) {
            for (Map.Entry<String, Object> e : ctx.getInput().entrySet()) {
                String token = "{{input." + e.getKey() + "}}";
                if (result.contains(token)) {
                    result = result.replace(token, e.getValue() == null ? "" : String.valueOf(e.getValue()));
                }
            }
        }
        return result;
    }

    private Double readDouble(NodeContext ctx, String key, double def) {
        Object v = ctx.getConfig(key);
        if (v == null) return def;
        if (v instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
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
}
