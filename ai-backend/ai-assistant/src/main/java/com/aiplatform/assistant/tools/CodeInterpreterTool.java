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
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");
            if (engine == null) return ToolResult.fail("当前 JDK 不支持 JavaScript 引擎");
            Object result = engine.eval(code);
            return ToolResult.ok(String.valueOf(result));
        } catch (ScriptException e) {
            log.warn("CodeInterpreterTool error: code={}", code, e);
            return ToolResult.fail("执行失败: " + e.getMessage());
        } catch (Exception e) {
            log.warn("CodeInterpreterTool unexpected error", e);
            return ToolResult.fail("执行失败: " + e.getMessage());
        }
    }
}
