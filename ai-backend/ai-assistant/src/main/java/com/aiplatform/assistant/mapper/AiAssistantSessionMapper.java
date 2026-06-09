package com.aiplatform.assistant.mapper;

import com.aiplatform.assistant.entity.AiAssistantSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiAssistantSessionMapper extends BaseMapper<AiAssistantSession> {

    @Select("SELECT * FROM ai_assistant_session WHERE assistant_id = #{assistantId} ORDER BY update_time DESC")
    List<AiAssistantSession> selectByAssistantId(@Param("assistantId") Long assistantId);

    @Select("SELECT * FROM ai_assistant_session WHERE user_id = #{userId} ORDER BY update_time DESC")
    List<AiAssistantSession> selectByUserId(@Param("userId") Long userId);
}
