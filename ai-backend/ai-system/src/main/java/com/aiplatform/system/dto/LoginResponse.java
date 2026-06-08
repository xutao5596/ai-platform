package com.aiplatform.system.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class LoginResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String tokenType = "Bearer";
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private UserInfo user;

    @Data
    public static class UserInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private String username;
        private String realName;
        private String nickname;
        private String avatar;
        private String email;
        private String phone;
        private Long deptId;
        private String deptName;
        private Boolean admin;
        private List<String> roles;
        private List<String> permissions;
    }
}
