package com.aiplatform.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class ApiKeySaveRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "名称不能为空")
    private String name;

    /** 授权 scope 列表,如 ["flow:run","assistant:chat"];空表示不限制 */
    private List<String> scopes;

    /** 每分钟请求限额(<=0 用默认 60) */
    private Integer rateLimit;

    /** 过期时间(epoch millis,毫秒);null 表示永不过期 */
    private Long expiresAt;

    /** 1=启用,0=禁用 */
    private Integer status;
}
