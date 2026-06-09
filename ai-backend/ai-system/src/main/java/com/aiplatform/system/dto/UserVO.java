package com.aiplatform.system.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class UserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String realName;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private Integer gender;
    private Long deptId;
    private String deptName;
    private Integer status;
    private Integer admin;
    private String remark;
    private List<Long> roleIds;
    private List<String> roleCodes;
    private String createTime;
}
