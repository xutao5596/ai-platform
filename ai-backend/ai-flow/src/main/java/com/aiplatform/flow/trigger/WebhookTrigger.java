package com.aiplatform.flow.trigger;

import com.aiplatform.flow.entity.AiFlowTrigger;
import com.aiplatform.flow.mapper.AiFlowTriggerMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Webhook 触发器:由 WebhookController 接收外部 HTTP 调用,根据 token 找到 flow。
 */
@Slf4j
@Component
public class WebhookTrigger implements FlowTrigger {

    private final TriggerRegistry registry;
    private final AiFlowTriggerMapper triggerMapper;

    public WebhookTrigger(@Lazy TriggerRegistry registry, AiFlowTriggerMapper triggerMapper) {
        this.registry = registry;
        this.triggerMapper = triggerMapper;
    }

    @Override
    public String getType() {
        return "webhook";
    }

    @Override
    public void trigger(Long flowId, Map<String, Object> input) {
        log.debug("WebhookTrigger.trigger: flowId={}", flowId);
        registry.executeSync(flowId, input, getType());
    }

    /**
     * 通过 token 找到 flow:token 是 ai_flow_trigger.config.token 字段
     */
    public Long resolveToken(String token) {
        if (token == null || token.isBlank()) return null;
        AiFlowTrigger t = triggerMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiFlowTrigger>()
                        .like(AiFlowTrigger::getConfig, "\"" + token + "\"")
                        .eq(AiFlowTrigger::getType, "webhook")
                        .eq(AiFlowTrigger::getStatus, 1)
                        .last("LIMIT 1"));
        if (t == null) return null;
        return t.getFlowId();
    }

    /**
     * 为新创建�?webhook 触发器生�?token
     */
    public static String generateToken() {
        return "wh_" + java.util.UUID.randomUUID().toString().replace("-", "");
    }
}
