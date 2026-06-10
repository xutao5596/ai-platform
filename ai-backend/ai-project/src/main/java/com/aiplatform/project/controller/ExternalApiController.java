package com.aiplatform.project.controller;

import com.aiplatform.common.api.Result;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import com.aiplatform.framework.security.ApiKeyContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * 外部 API 端点(API Key 鉴权,非 JWT)。
 * 端点:
 *  - GET  /api/v1/ext/flow/list?projectId=1
 *  - POST /api/v1/ext/flow/{id}/run
 *  - POST /api/v1/ext/assistant/{id}/chat
 * 本期只验证 ApiKeyAuthFilter + RateLimitFilter 链路可跑通,
 * 真实业务调用后续接 FlowRunService / AssistantChatService。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ext")
public class ExternalApiController {

    @GetMapping("/flow/list")
    public Result<List<Map<String, Object>>> listFlows(@RequestParam Long projectId) {
        requireSameProject(projectId);
        // 占位数据,后续替换为 FlowService.listByProject(经 ApiKey 鉴权后的简化调用)
        List<Map<String, Object>> rows = LongStream.rangeClosed(1, 3)
                .mapToObj(i -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", i);
                    m.put("name", "示例流程-" + i);
                    return m;
                })
                .collect(Collectors.toList());
        return Result.ok(rows);
    }

    @PostMapping("/flow/{id}/run")
    public Result<Map<String, Object>> runFlow(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        // 占位:仅验证链路
        Map<String, Object> out = new HashMap<>();
        out.put("flowId", id);
        out.put("runId", System.currentTimeMillis());
        out.put("trigger", "external");
        out.put("status", "accepted");
        out.put("input", body);
        return Result.ok(out);
    }

    @PostMapping("/assistant/{id}/chat")
    public Result<Map<String, Object>> chatAssistant(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (body == null || body.get("message") == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "message 不能为空");
        }
        Map<String, Object> out = new HashMap<>();
        out.put("assistantId", id);
        out.put("message", String.valueOf(body.get("message")));
        out.put("reply", "Sprint 4 占位:助手回复(API Key 链路已通)");
        out.put("apiKeyId", ApiKeyContext.getApiKeyId());
        return Result.ok(out);
    }

    private void requireSameProject(Long projectId) {
        Long ctx = ApiKeyContext.getProjectId();
        if (ctx == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "缺少 API Key 上下文");
        }
        if (projectId == null || !ctx.equals(projectId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该项目");
        }
    }
}
