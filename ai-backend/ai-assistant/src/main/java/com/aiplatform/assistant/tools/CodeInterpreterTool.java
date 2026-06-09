package com.aiplatform.assistant.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Map;

/**
 * JavaScript 代码执行工具(Sprint 3.1 PoC):运行一段 JS 表达式并返回结果。
 * 使用 JDK 自带 Nashorn 引擎(Java 21 已弃用但仍可用);生产环境建议 GraalJS。
 */
@Slf4j
@Component
public class CodeInterpreterTool implements AssistantTool {

    @Override
    public String name() {
        return "code_run";
    }

    @Override
    public String displayName() {
        return "JavaScript 执行";
    }

    @Override
    public String description() {
        return "执行一段 JavaScript 表达式并返回最后一个表达式的值,例如 '1+2' 或 'Math.sqrt(16)'。";
    }

    @Override
    public String category() {
        return "工具";
    }

    @Override
    public String parametersSchema() {
        return "{\"code\":\"string,required\"}";
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext context) {
        String code = (String) args.get("code");
        if (code == null || code.isBlank()) return ToolResult.fail("code 不能为空");
        try {
            // Java 21 已弃用 Nashorn 引擎,改用 SpEL 求值
            org.springframework.expression.ExpressionParser parser = new org.springframework.expression.spel.standard.SpelExpressionParser();
            org.springframework.expression.EvaluationContext ec = new org.springframework.expression.spel.support.StandardEvaluationContext();
            // 注册常用数学函数
            ec.setVariable("Math", Math.class);
            Object result = parser.parseExpression(code).getValue(ec);
            return ToolResult.ok(String.valueOf(result));
        } catch (Exception e) {
            log.warn("CodeInterpreterTool error: code={}", code, e);
            return ToolResult.fail("执行失败: " + e.getMessage());
        }
    }
}
