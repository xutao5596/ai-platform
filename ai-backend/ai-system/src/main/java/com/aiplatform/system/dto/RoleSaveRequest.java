package com.aiplatform.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class RoleSaveRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "角色名不能为空")
    private String name;

    @NotBlank(message = "角色编码不能为空")
    private String code;

    private String description;
    private Integer status;
    private Integer dataScope;
    private Integer sortOrder;
    private List<Long> menuIds;
    private List<String> permissions;
}
