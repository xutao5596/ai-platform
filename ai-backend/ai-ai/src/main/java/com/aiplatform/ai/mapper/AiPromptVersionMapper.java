package com.aiplatform.ai.mapper;

import com.aiplatform.ai.entity.AiPromptVersion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AiPromptVersionMapper extends BaseMapper<AiPromptVersion> {

    @Select("SELECT * FROM ai_prompt_version WHERE prompt_id = #{promptId} ORDER BY version DESC")
    List<AiPromptVersion> selectByPromptId(@Param("promptId") Long promptId);

    @Select("SELECT * FROM ai_prompt_version WHERE prompt_id = #{promptId} AND is_active = 1 LIMIT 1")
    AiPromptVersion selectActive(@Param("promptId") Long promptId);

    @Select("SELECT MAX(version) FROM ai_prompt_version WHERE prompt_id = #{promptId}")
    Integer maxVersion(@Param("promptId") Long promptId);

    @Update("UPDATE ai_prompt_version SET is_active = 0 WHERE prompt_id = #{promptId}")
    int deactivateAll(@Param("promptId") Long promptId);
}
