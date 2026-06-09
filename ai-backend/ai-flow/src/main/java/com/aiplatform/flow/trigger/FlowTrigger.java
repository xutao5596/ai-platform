package com.aiplatform.flow.trigger;

import java.util.Map;

/**
 * 流程触发器 SPI。
 * 实现:ManualTrigger / CronTrigger / WebhookTrigger / EventTrigger / ChainedTrigger
 */
public interface FlowTrigger {

    /** 触发器类型:manual / cron / webhook / event / chained */
    String getType();

    /** 触发执行。fire-and-forget:实现内部异步执行。 */
    void trigger(Long flowId, Map<String, Object> input);
}
