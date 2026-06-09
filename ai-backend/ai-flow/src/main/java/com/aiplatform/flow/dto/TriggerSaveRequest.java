package com.aiplatform.flow.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class TriggerSaveRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long flowId;
    private Long projectId;
    /** manual / cron / webhook / event / chained */
    private String type;
    /** JSON 配置 */
    private String config;
    private Integer status;
}
