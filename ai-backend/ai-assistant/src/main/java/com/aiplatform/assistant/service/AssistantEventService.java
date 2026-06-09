package com.aiplatform.assistant.service;

import com.aiplatform.ai.entity.AiAssistantConfig;
import com.aiplatform.ai.entity.AiAssistantEventSub;
import com.aiplatform.assistant.dto.EventSubRequest;
import com.aiplatform.ai.mapper.AiAssistantConfigMapper;
import com.aiplatform.ai.mapper.AiAssistantEventSubMapper;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import com.aiplatform.project.security.ProjectRoleChecker;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 助手事件订阅服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantEventService {

    private final AiAssistantEventSubMapper mapper;
    private final AiAssistantConfigMapper configMapper;
    private final ProjectRoleChecker projectRoleChecker;

    public List<AiAssistantEventSub> listByAssistant(Long assistantId) {
        // 验证助手存在 + 权限
        AiAssistantConfig a = configMapper.selectById(assistantId);
        if (a == null) throw new BusinessException(ErrorCode.NOT_FOUND, "助手不存在");
        projectRoleChecker.requireAtLeast(a.getProjectId(), "viewer");
        return mapper.selectList(new LambdaQueryWrapper<AiAssistantEventSub>()
                .eq(AiAssistantEventSub::getAssistantId, assistantId)
                .orderByDesc(AiAssistantEventSub::getCreateTime));
    }

    @Transactional
    public Long subscribe(Long assistantId, EventSubRequest req) {
        if (req.getEventType() == null || req.getEventType().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "eventType 不能为空");
        }
        AiAssistantConfig a = configMapper.selectById(assistantId);
        if (a == null) throw new BusinessException(ErrorCode.NOT_FOUND, "助手不存在");
        projectRoleChecker.requireAtLeast(a.getProjectId(), "developer");

        // 已存在则更新,否则新增
        AiAssistantEventSub exist = mapper.selectOne(new LambdaQueryWrapper<AiAssistantEventSub>()
                .eq(AiAssistantEventSub::getAssistantId, assistantId)
                .eq(AiAssistantEventSub::getEventType, req.getEventType()));
        if (exist != null) {
            exist.setFilter(req.getFilter());
            exist.setEnabled(req.getEnabled() == null ? 1 : req.getEnabled());
            exist.setDescription(req.getDescription());
            mapper.updateById(exist);
            return exist.getId();
        }
        AiAssistantEventSub e = new AiAssistantEventSub();
        e.setProjectId(a.getProjectId());
        e.setAssistantId(assistantId);
        e.setEventType(req.getEventType());
        e.setFilter(req.getFilter());
        e.setEnabled(req.getEnabled() == null ? 1 : req.getEnabled());
        e.setDescription(req.getDescription());
        e.setCreateTime(LocalDateTime.now());
        mapper.insert(e);
        log.info("Subscribe event: assistantId={}, type={}", assistantId, req.getEventType());
        return e.getId();
    }

    @Transactional
    public void unsubscribe(Long assistantId, Long eventId) {
        AiAssistantConfig a = configMapper.selectById(assistantId);
        if (a == null) throw new BusinessException(ErrorCode.NOT_FOUND, "助手不存在");
        projectRoleChecker.requireAtLeast(a.getProjectId(), "developer");
        AiAssistantEventSub e = mapper.selectById(eventId);
        if (e == null || !e.getAssistantId().equals(assistantId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "事件订阅不存在");
        }
        mapper.deleteById(eventId);
    }
}
