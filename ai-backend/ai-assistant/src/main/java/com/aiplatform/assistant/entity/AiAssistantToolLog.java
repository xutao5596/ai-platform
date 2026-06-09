package com.aiplatform.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 助手工具调用日志(系统工具执行记录)。
 */
@Data
@TableName("ai_assistant_tool_log")
public class AiAssistantToolLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assistantId;
    private Long sessionId;
    private String toolName;
    private String args;
    private String result;
    private Integer status;
    private Integer costMs;
    private String errorMsg;

    @TableField("create_time")
    private LocalDateTime createTime;
}
