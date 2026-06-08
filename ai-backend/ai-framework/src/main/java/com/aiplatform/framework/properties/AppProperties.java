package com.aiplatform.framework.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 应用配置(由 application.yml 映射)。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.app")
public class AppProperties {

    private String name = "AI Platform";
    private String version = "1.0.0";
    private String uploadPath = "/data/upload";
    private String hnswPath = "/data/hnsw";
    private String logPath = "/data/logs";
    private boolean demoMode = false;
}
