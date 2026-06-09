package com.aiplatform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@MapperScan(basePackages = {
        "com.aiplatform.system.mapper",
        "com.aiplatform.project.mapper",
        "com.aiplatform.ai.mapper",
        "com.aiplatform.flow.mapper",
        "com.aiplatform.assistant.mapper"
})
@SpringBootApplication(scanBasePackages = "com.aiplatform")
public class AiPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiPlatformApplication.class, args);
    }
}
