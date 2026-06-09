package com.aiplatform.flow.mapper;

import com.aiplatform.flow.entity.AiFlowVersion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AiFlowVersionMapper extends BaseMapper<AiFlowVersion> {

    @Select("SELECT * FROM ai_flow_version WHERE flow_id = #{flowId} ORDER BY version DESC")
    List<AiFlowVersion> selectByFlowId(@Param("flowId") Long flowId);

    @Select("SELECT * FROM ai_flow_version WHERE flow_id = #{flowId} AND is_active = 1 LIMIT 1")
    AiFlowVersion selectActive(@Param("flowId") Long flowId);

    @Select("SELECT MAX(version) FROM ai_flow_version WHERE flow_id = #{flowId}")
    Integer maxVersion(@Param("flowId") Long flowId);

    @Update("UPDATE ai_flow_version SET is_active = 0 WHERE flow_id = #{flowId}")
    int deactivateAll(@Param("flowId") Long flowId);
}
