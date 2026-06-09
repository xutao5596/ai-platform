package com.aiplatform.flow.trigger;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 链式触发器:由其他服务(如助手 / 子流程)内部调用 FlowTriggerRegistry.fire("chained", ...) 触发。
 */
@Slf4j
@Component
public class ChainedTrigger implements FlowTrigger {

    private final TriggerRegistry registry;

    public ChainedTrigger(@Lazy TriggerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getType() {
        return "chained";
    }

    @Override
    public void trigger(Long flowId, Map<String, Object> input) {
        log.debug("ChainedTrigger.trigger: flowId={}", flowId);
        registry.executeSync(flowId, input, getType());
    }
}
