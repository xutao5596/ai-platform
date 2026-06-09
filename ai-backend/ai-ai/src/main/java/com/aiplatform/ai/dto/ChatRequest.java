package com.aiplatform.ai.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class ChatRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long sessionId;
    private String title;
    private Long projectId;
    private Long modelId;
    private String systemPrompt;
    private java.math.BigDecimal temperature;
    private Integer maxTokens;
    private String kbIds;
    private String message;
    private Boolean stream;
    private List<java.util.Map<String, Object>> history;
}
