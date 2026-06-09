package com.aiplatform.assistant.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 助手配置保存请求。
 */
@Data
public class AssistantSaveRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private Long flowId;
    private String name;
    private String description;
    private String avatar;
    private String persona;
    private Long modelId;
    private BigDecimal temperature;
    private String kbIds;
    private String toolsEnabled;
    private String welcomeMsg;
    private Integer status;
}
