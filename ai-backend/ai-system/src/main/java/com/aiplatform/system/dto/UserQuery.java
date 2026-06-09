package com.aiplatform.system.dto;

import com.aiplatform.common.api.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserQuery extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    private String keyword;
    private Long deptId;
    private Integer status;
}
