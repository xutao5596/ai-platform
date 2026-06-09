package com.aiplatform.ai.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class ChatResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long sessionId;
    private Long messageId;
    private String content;
    private String reasoning;
    private List<ChatReference> references;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer costMs;

    @Data
    public static class ChatReference implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String docName;
        private String chunk;
        private Float score;
    }
}
