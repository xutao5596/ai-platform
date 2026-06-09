package com.aiplatform.ai.mapper;

import com.aiplatform.ai.entity.AiKnowledgeDoc;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AiKnowledgeDocMapper extends BaseMapper<AiKnowledgeDoc> {

    @Update("UPDATE ai_knowledge_doc SET status = #{status}, progress = #{progress}, error_msg = #{errorMsg} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("progress") Integer progress, @Param("errorMsg") String errorMsg);

    @Update("UPDATE ai_knowledge_doc SET status = 'ready', progress = 100, chunk_count = #{chunkCount}, parse_time = NOW() WHERE id = #{id}")
    int markReady(@Param("id") Long id, @Param("chunkCount") Integer chunkCount);
}
