package com.aiplatform.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class PromptVersionRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long promptId;
    @NotBlank private String content;
    private String variables;
    private Long modelId;
    private java.math.BigDecimal temperature;
    private Integer maxTokens;
    private String changelog;
    private Boolean activate;
}
