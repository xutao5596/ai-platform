package com.aiplatform.assistant.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工具定义描述(对前端展示用)。
 */
@Data
public class ToolDefinition implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String name;
    private String displayName;
    private String description;
    private String category;
    private String parametersSchema;
    private boolean enabled;
}
