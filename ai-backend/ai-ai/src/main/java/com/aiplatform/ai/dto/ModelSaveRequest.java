package com.aiplatform.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class ModelSaveRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    @NotBlank(message = "名称不能为空")
    private String name;
    @NotBlank(message = "厂商不能为空")
    private String provider;
    @NotBlank(message = "模型标识不能为空")
    private String modelName;
    private String apiBase;
    @NotBlank(message = "API Key 不能为空")
    private String apiKey;
    private Integer proxyEnabled;
    private String proxyUrl;
    private Integer maxTokens;
    private BigDecimal temperature;
    private BigDecimal topP;
    private String embeddingModel;
    private Integer dimension;
    private Integer status;
    private Integer isDefault;
    private String description;
}
