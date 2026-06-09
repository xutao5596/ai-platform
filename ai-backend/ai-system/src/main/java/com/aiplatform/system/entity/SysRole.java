package com.aiplatform.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_role")
public class SysRole implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String code;
    private String description;
    private Integer status;
    private Integer dataScope;
    private Integer sortOrder;

    @TableField(value = "create_by", fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private String createBy;
    @TableField(value = "create_time", fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_by", fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private String updateBy;
    @TableField(value = "update_time", fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableField("deleted")
    @com.baomidou.mybatisplus.annotation.TableLogic
    private Integer deleted;
}
