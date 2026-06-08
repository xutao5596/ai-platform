package com.aiplatform.project.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ProjectVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String code;
    private String description;
    private String icon;
    private Integer status;
    private Long ownerId;
    private String roleCode;
    private Integer memberCount;
    private Integer flowCount;
    private LocalDateTime createTime;
}
