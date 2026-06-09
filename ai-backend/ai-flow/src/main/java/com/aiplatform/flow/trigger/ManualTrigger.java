package com.aiplatform.flow.trigger;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 手动触发器:由用户在前端点击"运行"时触发。实际执行交给 TriggerRegistry 同步执行。
 */
@Slf4j
@Component
public class ManualTrigger implements FlowTrigger {

    private final TriggerRegistry registry;

    public ManualTrigger(@Lazy TriggerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getType() {
        return "manual";
    }

    @Override
    public void trigger(Long flowId, Map<String, Object> input) {
        log.debug("ManualTrigger.trigger: flowId={}", flowId);
        // 手动触发器:同步执行并返回结果
        registry.executeSync(flowId, input, getType());
    }
}
