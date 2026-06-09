package com.aiplatform.assistant.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 助手对话请求。
 */
@Data
public class AssistantChatRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long sessionId;
    private String message;
    private Boolean toolsEnabled;
    private List<String> toolNames;
    private String title;
}
