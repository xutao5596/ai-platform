package com.aiplatform.flow.service;

import com.aiplatform.flow.dto.CustomNodeSaveRequest;
import com.aiplatform.flow.entity.AiCustomNode;
import com.aiplatform.flow.mapper.AiCustomNodeMapper;
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
 * 自定义节点管理。
 */
@Service
@RequiredArgsConstructor
public class CustomNodeService {

    private final AiCustomNodeMapper customNodeMapper;
    private final ProjectRoleChecker projectRoleChecker;

    public PageResult<AiCustomNode> page(Long projectId, String keyword, long current, long size) {
        Page<AiCustomNode> page = new Page<>(current, size);
        LambdaQueryWrapper<AiCustomNode> w = new LambdaQueryWrapper<>();
        if (projectId != null) w.eq(AiCustomNode::getProjectId, projectId);
        if (keyword != null && !keyword.isBlank()) {
            w.and(z -> z.like(AiCustomNode::getName, keyword).or().like(AiCustomNode::getTypeKey, keyword));
        }
        w.orderByDesc(AiCustomNode::getCreateTime);
        Page<AiCustomNode> r = customNodeMapper.selectPage(page, w);
        return new PageResult<>(r.getCurrent(), r.getSize(), r.getTotal(), r.getRecords());
    }

    public List<AiCustomNode> listByProject(Long projectId) {
        return customNodeMapper.selectList(new LambdaQueryWrapper<AiCustomNode>()
                .eq(AiCustomNode::getProjectId, projectId)
                .eq(AiCustomNode::getStatus, 1)
                .orderByAsc(AiCustomNode::getName));
    }

    public AiCustomNode get(Long id) {
        AiCustomNode n = customNodeMapper.selectById(id);
        if (n == null) throw new BusinessException(ErrorCode.NOT_FOUND, "自定义节点不存在");
        return n;
    }

    @Transactional
    public Long create(CustomNodeSaveRequest req) {
        if (req.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(req.getProjectId(), "developer");
        }
        AiCustomNode n = new AiCustomNode();
        n.setProjectId(req.getProjectId());
        n.setName(req.getName());
        n.setTypeKey(req.getTypeKey());
        n.setCategory(req.getCategory());
        n.setConfigSchema(req.getConfigSchema());
        n.setImplementation(req.getImplementation());
        n.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        customNodeMapper.insert(n);
        return n.getId();
    }

    @Transactional
    public void update(CustomNodeSaveRequest req) {
        if (req.getId() == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "id 不能为空");
        AiCustomNode n = customNodeMapper.selectById(req.getId());
        if (n == null) throw new BusinessException(ErrorCode.NOT_FOUND, "自定义节点不存在");
        if (n.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(n.getProjectId(), "developer");
        }
        if (req.getName() != null) n.setName(req.getName());
        if (req.getTypeKey() != null) n.setTypeKey(req.getTypeKey());
        if (req.getCategory() != null) n.setCategory(req.getCategory());
        if (req.getConfigSchema() != null) n.setConfigSchema(req.getConfigSchema());
        if (req.getImplementation() != null) n.setImplementation(req.getImplementation());
        if (req.getStatus() != null) n.setStatus(req.getStatus());
        customNodeMapper.updateById(n);
    }

    @Transactional
    public void delete(Long id) {
        AiCustomNode n = customNodeMapper.selectById(id);
        if (n == null) return;
        if (n.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(n.getProjectId(), "admin");
        }
        customNodeMapper.deleteById(id);
    }
}
