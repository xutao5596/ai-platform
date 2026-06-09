package com.aiplatform.flow.nodes;

import com.aiplatform.ai.entity.AiPrompt;
import com.aiplatform.ai.entity.AiPromptVersion;
import com.aiplatform.ai.mapper.AiPromptMapper;
import com.aiplatform.ai.mapper.AiPromptVersionMapper;
import com.aiplatform.flow.spi.FlowNode;
import com.aiplatform.flow.spi.NodeContext;
import com.aiplatform.flow.spi.NodeExecuteResult;
import com.aiplatform.flow.spi.NodeSchema;
import com.aiplatform.flow.spi.Property;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 提示词模板节点:从 ai_prompt 读取激活版本,把 {{var}} 替换为 variables 中的值。
 *
 * 配置:
 *  - promptId: 提示词 ID(必填)
 *  - outputKey: 输出变量 Key(默认 "promptText")
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromptNode implements FlowNode {

    private final AiPromptMapper promptMapper;
    private final AiPromptVersionMapper versionMapper;

    @Override
    public String getTypeKey() {
        return "prompt";
    }

    @Override
    public String getDisplayName() {
        return "提示词模板";
    }

    @Override
    public String getCategory() {
        return "ai";
    }

    @Override
    public String getIcon() {
        return "mdi:text-box-edit";
    }

    @Override
    public String getColor() {
        return "#9C27B0";
    }

    @Override
    public String getDescription() {
        return "从提示词库中读取模板,替换 {{var}} 占位符,返回渲染后的文本。";
    }

    @Override
    public NodeSchema getSchema() {
        return NodeSchema.of(
                List.of(
                        new Property("promptId", "提示词", "prompt", true,
                                null, "选择提示词", null),
                        new Property("outputKey", "输出变量 Key", "string", false,
                                "promptText", "渲染结果写入 variables 的 key", null)
                ),
                List.of(
                        new Property("rendered", "渲染结果", "string", false, null, "模板渲染后的文本", null)
                )
        );
    }

    @Override
    public NodeExecuteResult execute(NodeContext ctx) {
        Long promptId = ctx.getConfigLong("promptId");
        if (promptId == null) {
            return NodeExecuteResult.fail("提示词节点缺少 promptId");
        }
        AiPrompt p = promptMapper.selectById(promptId);
        if (p == null) {
            return NodeExecuteResult.fail("提示词不存在: " + promptId);
        }
        // 取激活版本
        AiPromptVersion v = versionMapper.selectActive(promptId);
        if (v == null) {
            return NodeExecuteResult.fail("提示词无激活版本: " + promptId);
        }
        String template = v.getContent();
        String rendered = render(template, ctx);

        String outputKey = ctx.getConfigString("outputKey");
        if (outputKey == null || outputKey.isBlank()) outputKey = "promptText";

        Map<String, Object> out = new HashMap<>();
        out.put("rendered", rendered);
        out.put(outputKey, rendered);
        return NodeExecuteResult.success(out);
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
}
