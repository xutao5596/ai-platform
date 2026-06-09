package com.aiplatform.assistant.tools;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工具执行结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 是否成功 */
    private boolean success;
    /** 文本结果(直接给 LLM 看的总结) */
    private String output;
    /** 详细结果(JSON 字符串,可选) */
    private String detail;
    /** 错误信息(success=false 时) */
    private String error;

    public static ToolResult ok(String output) {
        return new ToolResult(true, output, null, null);
    }

    public static ToolResult ok(String output, String detail) {
        return new ToolResult(true, output, detail, null);
    }

    public static ToolResult fail(String error) {
        return new ToolResult(false, null, null, error);
    }
}
