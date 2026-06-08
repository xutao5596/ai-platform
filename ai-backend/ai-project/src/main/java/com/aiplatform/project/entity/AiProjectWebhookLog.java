package com.aiplatform.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ai_project_webhook_log")
public class AiProjectWebhookLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long webhookId;
    private Long projectId;
    private String event;
    private String requestUrl;
    private Integer responseStatus;
    private String responseBody;
    private String requestPayload;
    private Integer retryCount;
    private Long costMs;
    private LocalDateTime createTime;
}
