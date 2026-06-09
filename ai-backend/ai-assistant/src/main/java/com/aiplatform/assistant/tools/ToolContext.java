package com.aiplatform.assistant.tools;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工具执行上下文。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long assistantId;
    private Long sessionId;
    private Long userId;
    private Long projectId;
}
