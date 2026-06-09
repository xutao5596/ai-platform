package com.aiplatform.ai.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ai_chat_message")
public class AiChatMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;
    private String role;
    private String content;
    private String reasoning;
    private String toolCalls;
    private String toolCallId;
    private String name;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer costMs;
    private Integer status;
    private String errorMsg;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
