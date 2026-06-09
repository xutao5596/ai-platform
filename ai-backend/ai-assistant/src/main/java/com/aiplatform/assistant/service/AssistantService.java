package com.aiplatform.assistant.service;

import com.aiplatform.assistant.dto.AssistantSaveRequest;
import com.aiplatform.ai.entity.AiAssistantConfig;
import com.aiplatform.ai.mapper.AiAssistantConfigMapper;
import com.aiplatform.common.api.PageResult;
import com.aiplatform.common.context.UserContext;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import com.aiplatform.project.security.ProjectRoleChecker;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 助手配置 CRUD 服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantService {

    private final AiAssistantConfigMapper mapper;
    private final ProjectRoleChecker projectRoleChecker;

    public PageResult<AiAssistantConfig> page(Long projectId, String keyword, long current, long size) {
        if (projectId != null) {
            projectRoleChecker.requireAtLeast(projectId, "viewer");
        }
        Page<AiAssistantConfig> page = new Page<>(current, size);
        LambdaQueryWrapper<AiAssistantConfig> w = new LambdaQueryWrapper<>();
        if (projectId != null) w.eq(AiAssistantConfig::getProjectId, projectId);
        if (StringUtils.hasText(keyword)) {
            w.and(z -> z.like(AiAssistantConfig::getName, keyword)
                    .or().like(AiAssistantConfig::getDescription, keyword));
        }
        w.orderByDesc(AiAssistantConfig::getCreateTime);
        Page<AiAssistantConfig> r = mapper.selectPage(page, w);
        return new PageResult<>(r.getCurrent(), r.getSize(), r.getTotal(), r.getRecords());
    }

    public List<AiAssistantConfig> listByProject(Long projectId) {
        if (projectId != null) {
            projectRoleChecker.requireAtLeast(projectId, "viewer");
        }
        return mapper.selectList(new LambdaQueryWrapper<AiAssistantConfig>()
                .eq(projectId != null, AiAssistantConfig::getProjectId, projectId)
                .eq(AiAssistantConfig::getStatus, 1)
                .orderByDesc(AiAssistantConfig::getCreateTime));
    }

    public AiAssistantConfig get(Long id) {
        AiAssistantConfig a = mapper.selectById(id);
        if (a == null) throw new BusinessException(ErrorCode.NOT_FOUND, "助手不存在");
        if (a.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(a.getProjectId(), "viewer");
        }
        return a;
    }

    @Transactional
    public Long create(AssistantSaveRequest req) {
        if (req.getProjectId() == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "projectId 不能为空");
        projectRoleChecker.requireAtLeast(req.getProjectId(), "developer");
        if (req.getName() == null || req.getName().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "name 不能为空");
        }
        AiAssistantConfig a = new AiAssistantConfig();
        copy(req, a);
        if (a.getStatus() == null) a.setStatus(1);
        if (a.getTemperature() == null) a.setTemperature(new java.math.BigDecimal("0.70"));
        mapper.insert(a);
        log.info("Create assistant: id={}, name={}, project={}", a.getId(), a.getName(), a.getProjectId());
        return a.getId();
    }

    @Transactional
    public void update(AssistantSaveRequest req) {
        if (req.getId() == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "id 不能为空");
        AiAssistantConfig old = get(req.getId());
        projectRoleChecker.requireAtLeast(old.getProjectId(), "developer");
        copy(req, old);
        mapper.updateById(old);
    }

    @Transactional
    public void delete(Long id) {
        AiAssistantConfig old = get(id);
        projectRoleChecker.requireAtLeast(old.getProjectId(), "admin");
        mapper.deleteById(id);
    }

    private void copy(AssistantSaveRequest req, AiAssistantConfig a) {
        if (req.getId() != null) a.setId(req.getId());
        a.setProjectId(req.getProjectId());
        if (req.getFlowId() != null) a.setFlowId(req.getFlowId());
        a.setName(req.getName());
        a.setDescription(req.getDescription());
        a.setAvatar(req.getAvatar());
        a.setPersona(req.getPersona());
        a.setModelId(req.getModelId());
        a.setTemperature(req.getTemperature());
        a.setKbIds(req.getKbIds());
        a.setToolsEnabled(req.getToolsEnabled());
        a.setWelcomeMsg(req.getWelcomeMsg());
        if (req.getStatus() != null) a.setStatus(req.getStatus());
    }
}
