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
     * 通过 token 找到 flow:token = "wh_" + triggerId 随机串(简单实现)。
     * 这里为了简化,使用 trigger id 的 base36 编码作为 token。
     */
    public Long resolveToken(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            // 简化:token 是 trigger id(字符串)
            Long triggerId = Long.parseLong(token);
            AiFlowTrigger t = triggerMapper.selectById(triggerId);
            if (t == null || !"webhook".equals(t.getType())) return null;
            if (t.getStatus() == null || t.getStatus() != 1) return null;
            return t.getFlowId();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 为新创建的 webhook 触发器生成 token。 */
    public static String generateToken(Long triggerId) {
        return String.valueOf(triggerId);
    }
}
