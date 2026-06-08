package com.aiplatform.system.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class RoleVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String code;
    private String description;
    private Integer status;
    private Integer dataScope;
    private Integer sortOrder;
    private List<Long> menuIds;
    private List<String> permissions;
    private String createTime;
}
