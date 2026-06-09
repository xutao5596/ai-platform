package com.aiplatform.assistant.controller;

import com.aiplatform.assistant.dto.ToolDefinition;
import com.aiplatform.assistant.dto.ToolTestRequest;
import com.aiplatform.assistant.tools.AssistantTool;
import com.aiplatform.assistant.tools.ToolContext;
import com.aiplatform.assistant.tools.ToolRegistry;
import com.aiplatform.assistant.tools.ToolResult;
import com.aiplatform.common.api.Result;
import com.aiplatform.common.context.UserContext;
import com.aiplatform.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 助手工具管理:列表 + 测试执行。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/assistant/tools")
@RequiredArgsConstructor
public class AssistantToolController {

    private final ToolRegistry toolRegistry;

    @GetMapping
    public Result<List<ToolDefinition>> list() {
        return Result.ok(toolRegistry.definitions());
    }

    @PostMapping("/test")
    public Result<Map<String, Object>> test(@RequestBody ToolTestRequest req) {
        if (req.getToolName() == null || req.getToolName().isBlank()) {
            return Result.fail(400, "toolName 不能为空");
        }
        AssistantTool tool = toolRegistry.get(req.getToolName());
        if (tool == null) {
            return Result.fail(404, "工具不存在: " + req.getToolName());
        }
        long t0 = System.currentTimeMillis();
        ToolContext ctx = new ToolContext();
        ctx.setUserId(UserContext.getUserId());
        ToolResult r;
        try {
            r = tool.execute(req.getArgs() == null ? Map.of() : req.getArgs(), ctx);
        } catch (Exception e) {
            log.warn("Tool test error: {}", req.getToolName(), e);
            r = ToolResult.fail("执行异常: " + e.getMessage());
        }
        int cost = (int) (System.currentTimeMillis() - t0);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", r.isSuccess());
        resp.put("output", r.getOutput());
        resp.put("error", r.getError());
        resp.put("costMs", cost);
        resp.put("raw", JsonUtils.toJson(r));
        return Result.ok(resp);
    }
}
