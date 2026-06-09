package com.aiplatform.assistant.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 工具测试执行请求。
 */
@Data
public class ToolTestRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String toolName;
    private Map<String, Object> args;
}
