package com.aiplatform.flow.controller;

import com.aiplatform.common.api.Result;
import com.aiplatform.flow.entity.AiFlow;
import com.aiplatform.flow.entity.AiFlowRun;
import com.aiplatform.flow.mapper.AiFlowMapper;
import com.aiplatform.flow.mapper.AiFlowTriggerMapper;
import com.aiplatform.flow.trigger.TriggerRegistry;
import com.aiplatform.flow.entity.AiFlowTrigger;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 匿名 Webhook 端点(JwtAuthenticationFilter 已跳过 /api/v1/webhook/**)。
 * POST /api/v1/webhook/flow/{token}
 * token 解析为 trigger id,执行对应 flow。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/webhook/flow")
@RequiredArgsConstructor
public class WebhookController {

    private final AiFlowTriggerMapper triggerMapper;
    private final AiFlowMapper flowMapper;
    private final TriggerRegistry triggerRegistry;

    @PostMapping("/{token}")
    public Result<Map<String, Object>> receive(@PathVariable String token, @RequestBody(required = false) Map<String, Object> body) {
        Long triggerId;
        try {
            triggerId = Long.parseLong(token);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的 webhook token");
        }
        AiFlowTrigger t = triggerMapper.selectById(triggerId);
        if (t == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "webhook 不存在");
        }
        if (!"webhook".equals(t.getType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该 trigger 不是 webhook 类型");
        }
        if (t.getStatus() == null || t.getStatus() != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "webhook 已禁用");
        }
        AiFlow f = flowMapper.selectById(t.getFlowId());
        if (f == null) {
            throw new BusinessException(ErrorCode.FLOW_NOT_FOUND);
        }
        // 同步执行(便于 webhook 客户端等待结果)
        AiFlowRun run = triggerRegistry.executeSync(f.getId(), body == null ? new HashMap<>() : body, "webhook");
        Map<String, Object> resp = new HashMap<>();
        resp.put("runId", run.getId());
        resp.put("status", run.getStatus());
        resp.put("output", run.getOutput());
        return Result.ok(resp);
    }

    @GetMapping("/{token}/ping")
    public Result<Map<String, Object>> ping(@PathVariable String token) {
        return Result.ok(Map.of("ok", true, "token", token));
    }
}
