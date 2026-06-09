package com.aiplatform.flow.nodes;

import com.aiplatform.flow.spi.FlowNode;
import com.aiplatform.flow.spi.NodeContext;
import com.aiplatform.flow.spi.NodeExecuteResult;
import com.aiplatform.flow.spi.NodeSchema;
import com.aiplatform.flow.spi.Property;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 条件分支节点:用 SpEL 表达式求值,根据 true/false 路由。
 * 注:实际路由由设计器中节点的多出边配置承担,本节点仅设置 branch 变量供后续节点使用。
 */
@Slf4j
@Component
public class IfElseNode implements FlowNode {

    private final ExpressionParser parser = new SpelExpressionParser();

    @Override
    public String getTypeKey() {
        return "if_else";
    }

    @Override
    public String getDisplayName() {
        return "条件分支";
    }

    @Override
    public String getCategory() {
        return "control";
    }

    @Override
    public String getIcon() {
        return "mdi:call-split";
    }

    @Override
    public String getColor() {
        return "#F56C6C";
    }

    @Override
    public String getDescription() {
        return "基于 SpEL 表达式对 variables 求值,根据结果设置分支变量。";
    }

    @Override
    public NodeSchema getSchema() {
        return NodeSchema.of(
                List.of(
                        new Property("expression", "条件表达式", "string", true,
                                "1 == 1", "SpEL 表达式,基于 variables(例:#score > 60)", null),
                        new Property("trueKey", "true 变量 Key", "string", false,
                                "branchTrue", "求值为 true 时把哪个 key 写入 variables", null),
                        new Property("falseKey", "false 变量 Key", "string", false,
                                "branchFalse", "求值为 false 时把哪个 key 写入 variables", null)
                ),
                List.of(
                        new Property("result", "分支结果", "boolean", false, null, "求值结果", null),
                        new Property("branchKey", "命中的分支 Key", "string", false, null, "命中的 key", null)
                )
        );
    }

    @Override
    public NodeExecuteResult execute(NodeContext ctx) {
        String exprStr = ctx.getConfigString("expression");
        if (exprStr == null || exprStr.isBlank()) {
            return NodeExecuteResult.fail("条件分支节点缺少 expression");
        }
        String trueKey = ctx.getConfigString("trueKey");
        if (trueKey == null || trueKey.isBlank()) trueKey = "branchTrue";
        String falseKey = ctx.getConfigString("falseKey");
        if (falseKey == null || falseKey.isBlank()) falseKey = "branchFalse";

        try {
            EvaluationContext ec = new StandardEvaluationContext();
            if (ctx.getVariables() != null) {
                for (Map.Entry<String, Object> e : ctx.getVariables().entrySet()) {
                    ec.setVariable(e.getKey(), e.getValue());
                }
            }
            if (ctx.getInput() != null) {
                for (Map.Entry<String, Object> e : ctx.getInput().entrySet()) {
                    ec.setVariable("input." + e.getKey(), e.getValue());
                    ec.setVariable(e.getKey(), e.getValue());
                }
            }
            Expression exp = parser.parseExpression(exprStr);
            Object val = exp.getValue(ec);
            boolean result = Boolean.TRUE.equals(val);

            Map<String, Object> out = new HashMap<>();
            out.put("result", result);
            if (result) {
                out.put("branchKey", trueKey);
                ctx.putVariable(trueKey, true);
            } else {
                out.put("branchKey", falseKey);
                ctx.putVariable(falseKey, true);
            }
            return NodeExecuteResult.success(out);
        } catch (Exception e) {
            log.warn("IfElse 表达式求值失败: expr={}, err={}", exprStr, e.getMessage());
            return NodeExecuteResult.fail("条件求值失败: " + e.getMessage());
        }
    }
}
