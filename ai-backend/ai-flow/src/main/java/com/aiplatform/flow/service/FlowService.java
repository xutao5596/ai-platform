package com.aiplatform.flow.service;

import com.aiplatform.flow.chain.ChainBuilder;
import com.aiplatform.flow.dto.FlowSaveRequest;
import com.aiplatform.flow.entity.AiFlow;
import com.aiplatform.flow.mapper.AiFlowMapper;
import com.aiplatform.common.api.PageResult;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import com.aiplatform.project.security.ProjectRoleChecker;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 流程 CRUD。
 */
@Service
@RequiredArgsConstructor
public class FlowService {

    private final AiFlowMapper flowMapper;
    private final ChainBuilder chainBuilder;
    private final ProjectRoleChecker projectRoleChecker;

    public PageResult<AiFlow> page(Long projectId, String keyword, long current, long size) {
        Page<AiFlow> page = new Page<>(current, size);
        LambdaQueryWrapper<AiFlow> w = new LambdaQueryWrapper<>();
        if (projectId != null) w.eq(AiFlow::getProjectId, projectId);
        if (keyword != null && !keyword.isBlank()) {
            w.and(z -> z.like(AiFlow::getName, keyword).or().like(AiFlow::getDescription, keyword));
        }
        w.orderByDesc(AiFlow::getCreateTime);
        Page<AiFlow> r = flowMapper.selectPage(page, w);
        return new PageResult<>(r.getCurrent(), r.getSize(), r.getTotal(), r.getRecords());
    }

    public List<AiFlow> listByProject(Long projectId) {
        return flowMapper.selectList(new LambdaQueryWrapper<AiFlow>()
                .eq(AiFlow::getProjectId, projectId)
                .orderByDesc(AiFlow::getCreateTime));
    }

    public AiFlow get(Long id) {
        AiFlow f = flowMapper.selectById(id);
        if (f == null) throw new BusinessException(ErrorCode.FLOW_NOT_FOUND);
        return f;
    }

    public AiFlow getWithPermission(Long id, String requiredRole) {
        AiFlow f = get(id);
        if (f.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(f.getProjectId(), requiredRole);
        }
        return f;
    }

    @Transactional
    public Long create(FlowSaveRequest req) {
        if (req.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(req.getProjectId(), "developer");
        }
        AiFlow f = new AiFlow();
        f.setProjectId(req.getProjectId());
        f.setName(req.getName());
        f.setDescription(req.getDescription());
        f.setIcon(req.getIcon());
        f.setIsAssistant(req.getIsAssistant() == null ? 0 : req.getIsAssistant());
        f.setDesign(req.getDesign());
        f.setChain(generateChain(req.getDesign(), req.getChain()));
        f.setStatus(req.getStatus() == null ? "draft" : req.getStatus());
        flowMapper.insert(f);
        return f.getId();
    }

    @Transactional
    public void update(FlowSaveRequest req) {
        if (req.getId() == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "id 不能为空");
        AiFlow f = flowMapper.selectById(req.getId());
        if (f == null) throw new BusinessException(ErrorCode.FLOW_NOT_FOUND);
        if (f.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(f.getProjectId(), "developer");
        }
        if (req.getName() != null) f.setName(req.getName());
        if (req.getDescription() != null) f.setDescription(req.getDescription());
        if (req.getIcon() != null) f.setIcon(req.getIcon());
        if (req.getIsAssistant() != null) f.setIsAssistant(req.getIsAssistant());
        if (req.getDesign() != null) {
            f.setDesign(req.getDesign());
            f.setChain(generateChain(req.getDesign(), req.getChain()));
        } else if (req.getChain() != null) {
            f.setChain(req.getChain());
        }
        if (req.getStatus() != null) f.setStatus(req.getStatus());
        flowMapper.updateById(f);
    }

    @Transactional
    public void delete(Long id) {
        AiFlow f = flowMapper.selectById(id);
        if (f == null) return;
        if (f.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(f.getProjectId(), "admin");
        }
        flowMapper.deleteById(id);
    }

    private String generateChain(String design, String provided) {
        if (provided != null && !provided.isBlank()) return provided;
        if (design == null || design.isBlank()) return "";
        try {
            return chainBuilder.build(design).el();
        } catch (Exception e) {
            return "";
        }
    }
}
