package com.aiplatform.assistant.event;

import com.aiplatform.ai.entity.AiAssistantEventSub;
import com.aiplatform.ai.mapper.AiAssistantEventSubMapper;
import com.aiplatform.assistant.service.AssistantChatService;
import com.aiplatform.assistant.dto.AssistantChatRequest;
import com.aiplatform.common.context.UserContext;
import com.aiplatform.common.util.JsonUtils;
import com.aiplatform.flow.executor.FlowRunner;
import com.aiplatform.project.service.WebhookDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Flow 事件监听器:
 * 1. 触发已订阅该事件的助手(原 PoC 功能,保留);
 * 2. 触发已订阅该事件的外部 Webhook(Sprint 4 Agent B,经 WebhookDispatcher 异步分发)。
 *
 * 事件源为 FlowRunner.FlowRunSuccessEvent / FlowRunFailedEvent (含 runId/flowId/output/errorMsg,
 * 不含 projectId),内部通过 JdbcTemplate 查 ai_flow 拿 projectId。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlowEventListener {

    private final AiAssistantEventSubMapper subMapper;
    private final AssistantChatService chatService;
    private final WebhookDispatcher webhookDispatcher;
    private final JdbcTemplate jdbcTemplate;

    /** 兼容旧订阅路径:本类内部 record 形式,历史上没有发布者,保留以避免破坏编译。 */
    @Async
    @EventListener
    public void onFlowRunSuccess(FlowRunSuccessEvent event) {
        handleEvent("flow.run.success", event.payload(), event.projectId());
    }

    @Async
    @EventListener
    public void onFlowRunFailed(FlowRunFailedEvent event) {
        handleEvent("flow.run.failed", event.payload(), event.projectId());
    }

    /** Sprint 4 新增:订阅 FlowRunner 真实事件,触发 Webhook 出站分发。 */
    @Async
    @EventListener
    public void onFlowRunnerSuccess(FlowRunner.FlowRunSuccessEvent event) {
        handleWebhook("flow.run.success", event.flowId(), buildPayload(event.flowId(), event.runId(), event.output(), null));
    }

    @Async
    @EventListener
    public void onFlowRunnerFailed(FlowRunner.FlowRunFailedEvent event) {
        handleWebhook("flow.run.failed", event.flowId(), buildPayload(event.flowId(), event.runId(), null, event.errorMsg()));
    }

    private void handleEvent(String eventType, Map<String, Object> payload, Long projectId) {
        if (projectId == null) return;
        com.aiplatform.common.context.LoginUser sysUser = new com.aiplatform.common.context.LoginUser();
        sysUser.setUserId(0L);
        sysUser.setUsername("system");
        sysUser.setAdmin(true);
        com.aiplatform.common.context.UserContext.set(sysUser);
        try {
            List<AiAssistantEventSub> subs = subMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiAssistantEventSub>()
                            .eq(AiAssistantEventSub::getEventType, eventType)
                            .eq(AiAssistantEventSub::getEnabled, 1));
            if (subs.isEmpty()) return;

            String prompt = String.format("[系统通知] 收到事件: %s\n载荷: %s\n请用一句话总结并给出建议。",
                    eventType, JsonUtils.toJson(payload));

            for (AiAssistantEventSub sub : subs) {
                try {
                    AssistantChatRequest req = new AssistantChatRequest();
                    req.setMessage(prompt);
                    req.setToolsEnabled(false);
                    req.setTitle("[事件] " + eventType);
                    AssistantChatService.ChatOutcome out = chatService.chat(sub.getAssistantId(), req);
                    log.info("Event {} triggered assistant {}: {}", eventType, sub.getAssistantId(),
                            out.content() == null ? "" : out.content().substring(0, Math.min(80, out.content().length())));
                } catch (Exception e) {
                    log.warn("Assistant {} failed to handle event {}", sub.getAssistantId(), eventType, e);
                }
            }
        } catch (Exception e) {
            log.warn("handleEvent error: {}", eventType, e);
        } finally {
            com.aiplatform.common.context.UserContext.clear();
        }
    }

    private void handleWebhook(String eventType, Long flowId, Map<String, Object> payload) {
        Long projectId = resolveProjectId(flowId);
        if (projectId == null) {
            log.debug("Webhook dispatch skipped: cannot resolve projectId from flowId={}", flowId);
            return;
        }
        try {
            webhookDispatcher.dispatch(projectId, eventType, payload);
        } catch (Exception e) {
            log.warn("Webhook dispatch from FlowEventListener failed: flowId={}, event={}", flowId, eventType, e);
        }
    }

    private Long resolveProjectId(Long flowId) {
        if (flowId == null) return null;
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT project_id FROM ai_flow WHERE id = ? AND deleted = 0",
                    Long.class, flowId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (Exception e) {
            log.debug("resolveProjectId(flowId={}) failed: {}", flowId, e.getMessage());
            return null;
        }
    }

    private Map<String, Object> buildPayload(Long flowId, Long runId, Object output, String errorMsg) {
        Map<String, Object> m = new HashMap<>();
        m.put("flowId", flowId);
        m.put("runId", runId);
        if (output != null) m.put("output", output);
        if (errorMsg != null) m.put("error", errorMsg);
        return m;
    }

    /** 兼容保留:历史事件 record (无发布者,占位用)。 */
    public record FlowRunSuccessEvent(Long projectId, Long flowId, Long runId, Map<String, Object> payload) {
        public FlowRunSuccessEvent(Long projectId, Long flowId, Long runId) {
            this(projectId, flowId, runId, defaultPayload(flowId, runId));
        }
        private static Map<String, Object> defaultPayload(Long flowId, Long runId) {
            Map<String, Object> m = new HashMap<>();
            m.put("flowId", flowId);
            m.put("runId", runId);
            return m;
        }
    }

    public record FlowRunFailedEvent(Long projectId, Long flowId, Long runId, String errorMsg) {
        public Map<String, Object> payload() {
            Map<String, Object> m = new HashMap<>();
            m.put("flowId", flowId);
            m.put("runId", runId);
            m.put("error", errorMsg);
            return m;
        }
    }
}
