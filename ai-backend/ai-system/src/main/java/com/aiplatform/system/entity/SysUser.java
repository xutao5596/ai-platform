package com.aiplatform.system.entity;

import com.aiplatform.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
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
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private String remark;
}
