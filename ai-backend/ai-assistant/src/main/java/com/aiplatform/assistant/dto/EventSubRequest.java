package com.aiplatform.assistant.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 助手事件订阅保存请求。
 */
@Data
public class EventSubRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String eventType;
    private String filter;
    private Integer enabled;
    private String description;
}
