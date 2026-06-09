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
 * 助手专属会话(与 ai_chat_session 区分:带 tool_calls 工具调用字段)。
 */
@Data
@TableName("ai_assistant_session")
public class AiAssistantSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assistantId;
    private Long userId;
    private Long projectId;
    private String title;
    private Long modelId;
    private String systemPrompt;
    private Integer messageCount;
    private Integer status;

    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
}
