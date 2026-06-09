package com.aiplatform.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class MenuSaveRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long parentId;
    @NotBlank(message = "菜单名称不能为空")
    private String name;
    private String title;
    private String path;
    private String component;
    private String icon;
    private String permCode;
    private Integer type;
    private Integer sortOrder;
    private Integer visible;
    private Integer status;
    private String redirect;
    private String remark;
}
