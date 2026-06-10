package com.aiplatform.project.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ApiKeyVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    /** 完整 apiKey(明文) */
    private String apiKey;
    /** 完整 apiSecret(明文),仅创建/重置时返回 */
    private String apiSecret;
    /** 脱敏后的 secret(如 "sk_********xxxx") */
    private String maskedSecret;
    private List<String> scopes;
    private Integer rateLimit;
    private Long expiresAt;
    private Integer status;
    private LocalDateTime lastUsedTime;
    private String lastUsedIp;
    private LocalDateTime createdAt;
}
