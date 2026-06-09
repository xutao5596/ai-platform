package com.aiplatform.ai.service;

import com.aiplatform.ai.dto.PromptSaveRequest;
import com.aiplatform.ai.dto.PromptVersionRequest;
import com.aiplatform.ai.entity.AiPrompt;
import com.aiplatform.ai.entity.AiPromptVersion;
import com.aiplatform.ai.mapper.AiPromptMapper;
import com.aiplatform.ai.mapper.AiPromptVersionMapper;
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

@Service
@RequiredArgsConstructor
public class AiPromptService {

    private final AiPromptMapper promptMapper;
    private final AiPromptVersionMapper versionMapper;
    private final ProjectRoleChecker projectRoleChecker;

    public PageResult<AiPrompt> page(Long projectId, String keyword, long current, long size) {
        Page<AiPrompt> page = new Page<>(current, size);
        LambdaQueryWrapper<AiPrompt> w = new LambdaQueryWrapper<>();
        if (projectId != null) w.eq(AiPrompt::getProjectId, projectId);
        if (keyword != null && !keyword.isBlank()) {
            w.and(z -> z.like(AiPrompt::getName, keyword).or().like(AiPrompt::getCode, keyword));
        }
        w.orderByDesc(AiPrompt::getCreateTime);
        Page<AiPrompt> r = promptMapper.selectPage(page, w);
        return new PageResult<>(r.getCurrent(), r.getSize(), r.getTotal(), r.getRecords());
    }

    public List<AiPrompt> listByProject(Long projectId) {
        return promptMapper.selectList(new LambdaQueryWrapper<AiPrompt>()
                .eq(AiPrompt::getProjectId, projectId)
                .eq(AiPrompt::getStatus, 1)
                .orderByAsc(AiPrompt::getName));
    }

    public AiPrompt get(Long id) {
        AiPrompt p = promptMapper.selectById(id);
        if (p == null) throw new BusinessException(ErrorCode.NOT_FOUND, "提示词不存在");
        if (p.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(p.getProjectId(), "viewer");
        }
        return p;
    }

    public List<AiPromptVersion> listVersions(Long promptId) {
        AiPrompt p = promptMapper.selectById(promptId);
        if (p != null && p.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(p.getProjectId(), "viewer");
        }
        return versionMapper.selectByPromptId(promptId);
    }

    public AiPromptVersion getActiveVersion(Long promptId) {
        AiPrompt p = promptMapper.selectById(promptId);
        if (p != null && p.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(p.getProjectId(), "viewer");
        }
        return versionMapper.selectActive(promptId);
    }

    @Transactional
    public Long create(PromptSaveRequest req) {
        if (req.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(req.getProjectId(), "developer");
        }
        AiPrompt p = new AiPrompt();
        p.setProjectId(req.getProjectId());
        p.setName(req.getName());
        p.setCode(req.getCode());
        p.setDescription(req.getDescription());
        p.setStatus(1);
        p.setVersionCount(0);
        promptMapper.insert(p);
        return p.getId();
    }

    @Transactional
    public void update(PromptSaveRequest req) {
        if (req.getId() == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "id 不能为空");
        AiPrompt p = promptMapper.selectById(req.getId());
        if (p == null) throw new BusinessException(ErrorCode.NOT_FOUND, "提示词不存在");
        if (p.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(p.getProjectId(), "developer");
        }
        p.setName(req.getName());
        p.setCode(req.getCode());
        p.setDescription(req.getDescription());
        if (req.getStatus() != null) p.setStatus(req.getStatus());
        promptMapper.updateById(p);
    }

    @Transactional
    public void delete(Long id) {
        AiPrompt p = promptMapper.selectById(id);
        if (p != null && p.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(p.getProjectId(), "admin");
        }
        promptMapper.deleteById(id);
    }

    @Transactional
    public Long createVersion(PromptVersionRequest req) {
        AiPrompt p = promptMapper.selectById(req.getPromptId());
        if (p == null) throw new BusinessException(ErrorCode.NOT_FOUND, "提示词不存在");
        if (p.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(p.getProjectId(), "developer");
        }
        Integer max = versionMapper.maxVersion(req.getPromptId());
        int nextVersion = (max == null ? 0 : max) + 1;
        AiPromptVersion v = new AiPromptVersion();
        v.setPromptId(req.getPromptId());
        v.setProjectId(p.getProjectId());
        v.setVersion(nextVersion);
        v.setContent(req.getContent());
        v.setVariables(req.getVariables());
        v.setModelId(req.getModelId());
        v.setTemperature(req.getTemperature());
        v.setMaxTokens(req.getMaxTokens());
        v.setChangelog(req.getChangelog());
        v.setIsActive(Boolean.TRUE.equals(req.getActivate()) ? 1 : 0);
        versionMapper.insert(v);

        if (v.getIsActive() == 1) {
            versionMapper.deactivateAll(req.getPromptId());
            v.setIsActive(1);
            versionMapper.updateById(v);
            p.setCurrentVersionId(v.getId());
        }
        p.setVersionCount((p.getVersionCount() == null ? 0 : p.getVersionCount()) + 1);
        promptMapper.updateById(p);
        return v.getId();
    }

    @Transactional
    public void activateVersion(Long promptId, Long versionId) {
        AiPromptVersion v = versionMapper.selectById(versionId);
        if (v == null || !v.getPromptId().equals(promptId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "版本不存在");
        }
        versionMapper.deactivateAll(promptId);
        v.setIsActive(1);
        versionMapper.updateById(v);
        AiPrompt p = promptMapper.selectById(promptId);
        if (p != null) {
            if (p.getProjectId() != null) {
                projectRoleChecker.requireAtLeast(p.getProjectId(), "developer");
            }
            p.setCurrentVersionId(versionId);
            promptMapper.updateById(p);
        }
    }
}
