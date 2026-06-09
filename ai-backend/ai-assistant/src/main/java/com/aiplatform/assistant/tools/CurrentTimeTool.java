package com.aiplatform.assistant.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 当前时间工具。
 */
@Slf4j
@Component
public class CurrentTimeTool implements AssistantTool {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String name() {
        return "current_time";
    }

    @Override
    public String displayName() {
        return "当前时间";
    }

    @Override
    public String description() {
        return "返回服务器当前时间(Asia/Shanghai)。";
    }

    @Override
    public String category() {
        return "基础";
    }

    @Override
    public String parametersSchema() {
        return "{}";
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext context) {
        String now = LocalDateTime.now(ZoneId.of("Asia/Shanghai")).format(FMT);
        return ToolResult.ok("当前时间: " + now);
    }
}
