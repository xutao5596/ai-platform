package com.aiplatform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableAsync
@EnableTransactionManagement
@MapperScan(basePackages = {
        "com.aiplatform.system.mapper",
        "com.aiplatform.project.mapper",
        "com.aiplatform.ai.mapper"
})
@SpringBootApplication(scanBasePackages = "com.aiplatform")
public class AiPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiPlatformApplication.class, args);
    }
}
