package com.aiplatform.assistant.mapper;

import com.aiplatform.assistant.entity.AiAssistantMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiAssistantMessageMapper extends BaseMapper<AiAssistantMessage> {

    @Select("SELECT * FROM ai_assistant_message WHERE session_id = #{sessionId} ORDER BY create_time ASC, id ASC")
    List<AiAssistantMessage> selectBySessionId(@Param("sessionId") Long sessionId);
}
