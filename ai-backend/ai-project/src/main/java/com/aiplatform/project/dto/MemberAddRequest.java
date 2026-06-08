package com.aiplatform.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class MemberAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    @NotBlank(message = "角色不能为空")
    private String roleCode;
}
