package com.aiplatform.flow.trigger;

import com.aiplatform.common.util.JsonUtils;
import com.aiplatform.flow.entity.AiFlowTrigger;
import com.aiplatform.flow.executor.FlowRunner;
import com.aiplatform.flow.mapper.AiFlowTriggerMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程事件订阅器:监听 FlowRunner 发布的成功/失败事件,
 * 根据 ai_flow_trigger 表中订阅 eventTrigger 的 flowId 触发对应 flow。
 *
 * 独立类(不实现 FlowTrigger 接口),避免 @EventListener 在 JDK 代理下失效。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlowEventSubscriber {

    private final AiFlowTriggerMapper triggerMapper;
    private final TriggerRegistry registry;

    @Async
    @EventListener
    public void onFlowRunSuccess(FlowRunner.FlowRunSuccessEvent event) {
        handle(event.flowId(), "flow.run.success", buildPayload(event.runId(), "flow.run.success", event.output(), null));
    }

    @Async
    @EventListener
    public void onFlowRunFailed(FlowRunner.FlowRunFailedEvent event) {
        handle(event.flowId(), "flow.run.failed", buildPayload(event.runId(), "flow.run.failed", null, event.errorMsg()));
    }

    private void handle(Long srcFlowId, String eventType, Map<String, Object> payload) {
        try {
            // 简化:扫描所有订阅了该事件类型 eventTrigger 的 flow
            List<AiFlowTrigger> subs = triggerMapper.selectList(
                    new LambdaQueryWrapper<AiFlowTrigger>()
                            .eq(AiFlowTrigger::getType, "event")
                            .eq(AiFlowTrigger::getStatus, 1));
            for (AiFlowTrigger t : subs) {
                String cfg = t.getConfig();
                if (cfg == null || cfg.isBlank()) continue;
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> cfgMap = JsonUtils.fromJson(cfg, Map.class);
                    if (cfgMap == null) continue;
                    Object sub = cfgMap.get("eventType");
                    if (sub == null) continue;
                    if (!eventType.equals(String.valueOf(sub))) continue;
                    // 命中订阅
                    registry.fire("event", t.getFlowId(), payload);
                } catch (Exception ignore) {
                }
            }
        } catch (Exception e) {
            log.debug("FlowEventSubscriber 处理 {} 失败: {}", eventType, e.getMessage());
        }
    }

    private Map<String, Object> buildPayload(Long runId, String eventType, Object output, String errorMsg) {
        Map<String, Object> p = new HashMap<>();
        p.put("eventType", eventType);
        p.put("runId", runId);
        if (output != null) p.put("output", output);
        if (errorMsg != null) p.put("errorMsg", errorMsg);
        return p;
    }
}
