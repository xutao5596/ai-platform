package com.aiplatform.flow.service;

import com.aiplatform.flow.dto.FlowRunRequest;
import com.aiplatform.flow.entity.AiFlow;
import com.aiplatform.flow.entity.AiFlowRun;
import com.aiplatform.flow.entity.AiFlowRunStep;
import com.aiplatform.flow.executor.FlowRunner;
import com.aiplatform.flow.mapper.AiFlowMapper;
import com.aiplatform.flow.mapper.AiFlowRunMapper;
import com.aiplatform.flow.mapper.AiFlowRunStepMapper;
import com.aiplatform.common.api.PageResult;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import com.aiplatform.project.security.ProjectRoleChecker;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程运行管理。
 */
@Service
@RequiredArgsConstructor
public class FlowRunService {

    private final AiFlowMapper flowMapper;
    private final AiFlowRunMapper runMapper;
    private final AiFlowRunStepMapper stepMapper;
    private final FlowRunner flowExecutor;
    private final ProjectRoleChecker projectRoleChecker;

    public AiFlowRun run(Long flowId, FlowRunRequest req) {
        AiFlow f = flowMapper.selectById(flowId);
        if (f == null) throw new BusinessException(ErrorCode.FLOW_NOT_FOUND);
        if (f.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(f.getProjectId(), "viewer");
        }
        if (req == null) req = new FlowRunRequest();
        String trigger = req.getTriggerType() == null ? "manual" : req.getTriggerType();
        return flowExecutor.execute(f, req.getVersionId(), req.getInput(), trigger);
    }

    public PageResult<AiFlowRun> page(Long flowId, long current, long size) {
        Page<AiFlowRun> page = new Page<>(current, size);
        LambdaQueryWrapper<AiFlowRun> w = new LambdaQueryWrapper<>();
        if (flowId != null) w.eq(AiFlowRun::getFlowId, flowId);
        w.orderByDesc(AiFlowRun::getCreateTime);
        Page<AiFlowRun> r = runMapper.selectPage(page, w);
        return new PageResult<>(r.getCurrent(), r.getSize(), r.getTotal(), r.getRecords());
    }

    public AiFlowRun get(Long runId) {
        AiFlowRun r = runMapper.selectById(runId);
        if (r == null) throw new BusinessException(ErrorCode.NOT_FOUND, "运行记录不存在");
        return r;
    }

    public List<AiFlowRunStep> steps(Long runId) {
        return stepMapper.selectByRunId(runId);
    }

    /** 触发器测试入口 */
    public AiFlowRun testTrigger(Long flowId, String triggerType, Map<String, Object> input) {
        AiFlow f = flowMapper.selectById(flowId);
        if (f == null) throw new BusinessException(ErrorCode.FLOW_NOT_FOUND);
        if (f.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(f.getProjectId(), "developer");
        }
        return flowExecutor.execute(f, null, input, triggerType);
    }

    public Map<String, Object> getSummary(Long runId) {
        AiFlowRun r = get(runId);
        List<AiFlowRunStep> steps = steps(runId);
        Map<String, Object> out = new HashMap<>();
        out.put("run", r);
        out.put("steps", steps);
        return out;
    }
}
