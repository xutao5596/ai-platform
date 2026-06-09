package com.aiplatform.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class UserSaveRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 30, message = "用户名长度 3-30")
    private String username;

    private String password;

    private String realName;
    private String nickname;
    private String avatar;
    @Email(message = "邮箱格式错误")
    private String email;
    private String phone;
    private Integer gender;
    private Long deptId;
    private Integer status;
    private String remark;
    private List<Long> roleIds;
}
