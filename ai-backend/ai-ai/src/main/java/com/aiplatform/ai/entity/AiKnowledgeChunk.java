package com.aiplatform.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ai_knowledge_chunk")
public class AiKnowledgeChunk implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long docId;
    private Long kbId;
    private Long projectId;
    private Integer chunkIndex;
    private String content;
    private Integer contentLen;
    private Long vectorId;
    private Float score;
    private String metadata;

    @TableField("create_time")
    private LocalDateTime createTime;
}
