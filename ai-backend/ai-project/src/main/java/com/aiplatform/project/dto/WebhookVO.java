package com.aiplatform.project.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WebhookVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private String name;
    private String url;
    /**
     * 仅在创建或重置 secret 时返回明文;
     * 列表 / 更新响应为 null,前端需妥善保存。
     */
    private String secret;
    private List<String> events;
    private Integer status;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
