package com.aiplatform.ai.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ai_knowledge_doc")
public class AiKnowledgeDoc implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long kbId;
    private Long projectId;
    private String name;
    private String originalName;
    private String filePath;
    private Long fileSize;
    private String contentType;
    private String status;
    private Integer progress;
    private Integer chunkCount;
    private String errorMsg;
    private LocalDateTime parseTime;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
