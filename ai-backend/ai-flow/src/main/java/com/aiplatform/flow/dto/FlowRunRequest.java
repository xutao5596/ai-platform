package com.aiplatform.flow.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Data
public class FlowRunRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 流程入参 */
    private Map<String, Object> input;
    /** 是否异步(Sprint 3 简化为同步) */
    private Boolean async;
    /** 触发器类型:manual / cron / webhook / event / chained */
    private String triggerType;
    /** 指定 version id(可空,用当前 design) */
    private Long versionId;
}
