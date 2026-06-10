package com.aiplatform.framework.observability;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 可观测性配置(Sprint 5 Agent B)。
 * <p>
 * 角色:在 Spring 启动时把全局 {@link MeterRegistry} 注入到
 * {@link BusinessMetrics},使其能在静态方法中懒注册 Counter/Timer。
 * <p>
 * 注意:Filter 顺序由 {@link TraceIdFilter} 上的 {@code @Order(HIGHEST_PRECEDENCE)} 控制,
 * 本类仅负责指标绑定。
 */
@Component
@RequiredArgsConstructor
public class MetricsConfig {

    private final MeterRegistry meterRegistry;

    @PostConstruct
    public void init() {
        BusinessMetrics.setRegistry(meterRegistry);
    }
}
