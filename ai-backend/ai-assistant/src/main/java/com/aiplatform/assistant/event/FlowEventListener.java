package com.aiplatform.assistant.event;

import com.aiplatform.ai.entity.AiAssistantEventSub;
import com.aiplatform.ai.mapper.AiAssistantEventSubMapper;
import com.aiplatform.assistant.service.AssistantChatService;
import com.aiplatform.assistant.dto.AssistantChatRequest;
import com.aiplatform.common.context.UserContext;
import com.aiplatform.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Flow 事件监听器:监听 flow.run.success / flow.run.failed 事件,触发已订阅该事件的助手。
 * PoC:异步调用助手对话,把事件内容作为 user 消息传入。
 * 生产应支持更丰富的 prompt 模板 + 助手可配置回复风格。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlowEventListener {

    private final AiAssistantEventSubMapper subMapper;
    private final AssistantChatService chatService;

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

    private void handleEvent(String eventType, Map<String, Object> payload, Long projectId) {
        if (projectId == null) return;
        // 异步事件线程,设置 system 上下文(避免 projectRoleChecker 抛未登录)
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
                    req.setToolsEnabled(false); // 事件响应不调用工具(避免循环)
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

    /** Flow 运行成功事件(由 Agent A 的 FlowService 发布) */
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

    /** Flow 运行失败事件 */
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
