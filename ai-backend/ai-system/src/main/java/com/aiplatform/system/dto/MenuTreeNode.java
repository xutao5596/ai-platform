package com.aiplatform.system.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class MenuTreeNode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long parentId;
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
    private List<MenuTreeNode> children = new ArrayList<>();
}
