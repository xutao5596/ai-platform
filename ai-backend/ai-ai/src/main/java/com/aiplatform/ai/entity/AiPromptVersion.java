package com.aiplatform.ai.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ai_prompt_version")
public class AiPromptVersion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long promptId;
    private Long projectId;
    private Integer version;
    private String content;
    private String variables;
    private Long modelId;
    private BigDecimal temperature;
    private Integer maxTokens;
    private String changelog;
    private Integer isActive;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
