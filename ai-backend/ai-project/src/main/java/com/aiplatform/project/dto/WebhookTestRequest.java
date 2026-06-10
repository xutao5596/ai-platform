package com.aiplatform.project.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Data
public class WebhookTestRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 事件类型,默认 webhook.test */
    private String event = "webhook.test";

    /** 自定义 payload(可空,默认给个 hello 模板) */
    private Map<String, Object> payload;
}
