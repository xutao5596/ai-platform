package com.aiplatform.system.dto;

import com.aiplatform.common.api.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class LogQuery extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    private String module;
    private String action;
    private String username;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
