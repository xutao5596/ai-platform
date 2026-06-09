package com.aiplatform.flow.service;

import com.aiplatform.flow.dto.FlowSaveRequest;
import com.aiplatform.flow.dto.FlowVersionRequest;
import com.aiplatform.flow.entity.AiFlow;
import com.aiplatform.flow.entity.AiFlowVersion;
import com.aiplatform.flow.mapper.AiFlowMapper;
import com.aiplatform.flow.mapper.AiFlowVersionMapper;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import com.aiplatform.project.security.ProjectRoleChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 流程版本管理。
 */
@Service
@RequiredArgsConstructor
public class FlowVersionService {

    private final AiFlowMapper flowMapper;
    private final AiFlowVersionMapper versionMapper;
    private final ProjectRoleChecker projectRoleChecker;

    public List<AiFlowVersion> list(Long flowId) {
        return versionMapper.selectByFlowId(flowId);
    }

    public AiFlowVersion getActive(Long flowId) {
        return versionMapper.selectActive(flowId);
    }

    @Transactional
    public Long createVersion(Long flowId, FlowVersionRequest req) {
        AiFlow f = flowMapper.selectById(flowId);
        if (f == null) throw new BusinessException(ErrorCode.FLOW_NOT_FOUND);
        if (f.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(f.getProjectId(), "developer");
        }
        Integer max = versionMapper.maxVersion(flowId);
        int next = (max == null ? 0 : max) + 1;
        AiFlowVersion v = new AiFlowVersion();
        v.setFlowId(flowId);
        v.setProjectId(f.getProjectId());
        v.setVersion(next);
        v.setDesign(req.getDesign() == null ? f.getDesign() : req.getDesign());
        v.setChain(req.getChain() == null ? f.getChain() : req.getChain());
        v.setChangelog(req.getChangelog());
        v.setIsActive(0);
        versionMapper.insert(v);
        return v.getId();
    }

    @Transactional
    public void publish(Long flowId, Long versionId) {
        AiFlow f = flowMapper.selectById(flowId);
        if (f == null) throw new BusinessException(ErrorCode.FLOW_NOT_FOUND);
        if (f.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(f.getProjectId(), "admin");
        }
        AiFlowVersion v = versionMapper.selectById(versionId);
        if (v == null || !v.getFlowId().equals(flowId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "版本不存在");
        }
        versionMapper.deactivateAll(flowId);
        v.setIsActive(1);
        versionMapper.updateById(v);
        // 更新 flow 的 current_version_id 和 status
        f.setCurrentVersionId(versionId);
        f.setStatus("published");
        f.setDesign(v.getDesign());
        f.setChain(v.getChain());
        flowMapper.updateById(f);
    }
}
