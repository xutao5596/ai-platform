package com.aiplatform.ai.mapper;

import com.aiplatform.ai.entity.AiChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessage> {

    @Select("SELECT * FROM ai_chat_message WHERE session_id = #{sessionId} ORDER BY create_time ASC, id ASC")
    List<AiChatMessage> selectBySessionId(@Param("sessionId") Long sessionId);
}
