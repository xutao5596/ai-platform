package com.aiplatform.framework.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.jwt")
public class JwtProperties {

    private String secret = "ai-platform-jwt-secret-key-please-change-in-production-2026";
    private String issuer = "ai-platform";
    private long accessTtlSeconds = 7200L;
    private long refreshTtlSeconds = 2592000L;
    private String header = "Authorization";
    private String tokenPrefix = "Bearer ";
}
