package com.aiplatform.flow.mapper;

import com.aiplatform.flow.entity.AiFlowRunStep;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiFlowRunStepMapper extends BaseMapper<AiFlowRunStep> {

    @Select("SELECT * FROM ai_flow_run_step WHERE run_id = #{runId} ORDER BY id ASC")
    List<AiFlowRunStep> selectByRunId(@Param("runId") Long runId);
}
