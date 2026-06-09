package com.aiplatform.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class PromptSaveRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    @NotBlank private String name;
    @NotBlank private String code;
    private String description;
    private Integer status;
}
