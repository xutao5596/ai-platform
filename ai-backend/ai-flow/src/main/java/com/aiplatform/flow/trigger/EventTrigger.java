package com.aiplatform.flow.trigger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 事件触发器:实现 FlowTrigger 接口。
 * 实际事件监听由 FlowEventSubscriber 处理(避免 JDK 代理问题)。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventTrigger implements FlowTrigger {

    private final TriggerRegistry registry;

    @Override
    public String getType() {
        return "event";
    }

    @Override
    public void trigger(Long flowId, Map<String, Object> input) {
        log.debug("EventTrigger.trigger: flowId={}", flowId);
        registry.executeSync(flowId, input, getType());
    }
}
