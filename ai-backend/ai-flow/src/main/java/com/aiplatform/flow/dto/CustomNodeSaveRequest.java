package com.aiplatform.flow.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class CustomNodeSaveRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private String name;
    private String typeKey;
    private String category;
    private String configSchema;
    private String implementation;
    private Integer status;
}
