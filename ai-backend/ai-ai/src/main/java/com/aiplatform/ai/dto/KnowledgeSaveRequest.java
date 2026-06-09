package com.aiplatform.ai.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class KnowledgeSaveRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private String name;
    private String description;
    private Long modelId;
    private Integer chunkSize;
    private Integer chunkOverlap;
    private String sep;
    private Integer status;
}
