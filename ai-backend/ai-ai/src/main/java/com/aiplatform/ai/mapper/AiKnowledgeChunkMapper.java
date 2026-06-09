package com.aiplatform.ai.mapper;

import com.aiplatform.ai.entity.AiKnowledgeChunk;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiKnowledgeChunkMapper extends BaseMapper<AiKnowledgeChunk> {

    @Delete("DELETE FROM ai_knowledge_chunk WHERE doc_id = #{docId}")
    int deleteByDocId(@Param("docId") Long docId);

    @Delete("DELETE FROM ai_knowledge_chunk WHERE kb_id = #{kbId}")
    int deleteByKbId(@Param("kbId") Long kbId);
}
