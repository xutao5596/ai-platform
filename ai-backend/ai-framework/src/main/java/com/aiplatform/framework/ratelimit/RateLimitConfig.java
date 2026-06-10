package com.aiplatform.framework.ratelimit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 限流组件配置。
 */
@Configuration
public class RateLimitConfig {

    @Bean
    public RateLimiter rateLimiter() {
        return new RateLimiter(60);
    }
}
