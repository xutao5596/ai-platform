package com.aiplatform.framework.log;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志实体(sys_log 表对应)。
 * 定义在 framework 中供切面使用,由 ai-system 提供 mapper/service。
 */
@Data
public class OperationLogRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String module;
    private String action;
    private String method;
    private String requestUrl;
    private String requestMethod;
    private String requestParams;
    private String responseData;
    private Long userId;
    private String username;
    private String ip;
    private String userAgent;
    private Long costMs;
    private Integer status;
    private String errorMsg;
    private LocalDateTime createTime;
}
