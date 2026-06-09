package com.aiplatform.flow.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class FlowSaveRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private String name;
    private String description;
    private String icon;
    private Integer isAssistant;
    /** LogicFlow 设计 JSON */
    private String design;
    /** LiteFlow Chain(可空,后端自动生成) */
    private String chain;
    /** draft/published/archived */
    private String status;
}
