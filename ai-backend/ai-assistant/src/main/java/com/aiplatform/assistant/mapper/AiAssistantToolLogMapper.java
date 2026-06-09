package com.aiplatform.assistant.mapper;

import com.aiplatform.assistant.entity.AiAssistantToolLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiAssistantToolLogMapper extends BaseMapper<AiAssistantToolLog> {

    @Select("SELECT * FROM ai_assistant_tool_log WHERE assistant_id = #{assistantId} ORDER BY create_time DESC LIMIT #{limit}")
    List<AiAssistantToolLog> selectRecent(@Param("assistantId") Long assistantId, @Param("limit") int limit);
}
