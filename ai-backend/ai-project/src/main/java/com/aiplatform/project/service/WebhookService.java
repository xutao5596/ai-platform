package com.aiplatform.project.service;

import com.aiplatform.common.context.LoginUser;
import com.aiplatform.common.context.UserContext;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import com.aiplatform.common.util.IdUtils;
import com.aiplatform.common.util.JsonUtils;
import com.aiplatform.project.dto.WebhookSaveRequest;
import com.aiplatform.project.dto.WebhookVO;
import com.aiplatform.project.entity.AiProjectWebhook;
import com.aiplatform.project.event.WebhookEvent;
import com.aiplatform.project.mapper.AiProjectWebhookMapper;
import com.aiplatform.project.security.ProjectRoleChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Webhook 业务逻辑:CRUD + secret 生成。
 * 权限由调用方通过 @PreProjectRole 保证(WebhookReceiveController 是匿名端点,不走此 Service)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final AiProjectWebhookMapper webhookMapper;
    private final ProjectRoleChecker roleChecker;

    public List<WebhookVO> list(Long projectId) {
        roleChecker.requireAtLeast(projectId, "viewer");
        return webhookMapper.selectPageByProject(
                com.baomidou.mybatisplus.extension.plugins.pagination.Page.of(1, 1000), projectId)
                .getRecords().stream().map(this::toVO).collect(Collectors.toList());
    }

    public WebhookVO get(Long projectId, Long id) {
        roleChecker.requireAtLeast(projectId, "viewer");
        AiProjectWebhook w = mustGet(id, projectId);
        return toVO(w);
    }

    @Transactional
    public WebhookVO create(Long projectId, WebhookSaveRequest req) {
        roleChecker.requireAtLeast(projectId, "admin");
        AiProjectWebhook w = new AiProjectWebhook();
        w.setProjectId(projectId);
        w.setName(req.getName());
        w.setUrl(req.getUrl());
        w.setSecret(generateSecret());
        w.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        w.setDescription(req.getDescription());
        w.setEvents(serializeEvents(req.getEvents()));
        applyAudit(w, true);
        webhookMapper.insert(w);
        WebhookVO vo = toVO(w);
        log.info("Webhook created: id={}, projectId={}, name={}", w.getId(), projectId, w.getName());
        return vo;
    }

    @Transactional
    public WebhookVO update(Long projectId, WebhookSaveRequest req) {
        roleChecker.requireAtLeast(projectId, "admin");
        if (req.getId() == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "id 不能为空");
        AiProjectWebhook w = mustGet(req.getId(), projectId);
        w.setName(req.getName());
        w.setUrl(req.getUrl());
        if (req.getStatus() != null) w.setStatus(req.getStatus());
        w.setDescription(req.getDescription());
        w.setEvents(serializeEvents(req.getEvents()));
        applyAudit(w, false);
        webhookMapper.updateById(w);
        return toVO(w);
    }

    @Transactional
    public void delete(Long projectId, Long id) {
        roleChecker.requireAtLeast(projectId, "admin");
        AiProjectWebhook w = mustGet(id, projectId);
        webhookMapper.deleteById(w.getId());
    }

    /**
     * 重置 secret:生成新 secret,返回明文(仅此一次)。
     */
    @Transactional
    public WebhookVO resetSecret(Long projectId, Long id) {
        roleChecker.requireAtLeast(projectId, "admin");
        AiProjectWebhook w = mustGet(id, projectId);
        w.setSecret(generateSecret());
        applyAudit(w, false);
        webhookMapper.updateById(w);
        return toVO(w);
    }

    public AiProjectWebhook mustGet(Long id, Long projectId) {
        AiProjectWebhook w = webhookMapper.selectById(id);
        if (w == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Webhook 不存在");
        }
        if (projectId != null && !w.getProjectId().equals(projectId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Webhook 不存在");
        }
        return w;
    }

    private void applyAudit(AiProjectWebhook w, boolean create) {
        LoginUser u = UserContext.get();
        String name = u == null ? "system" : (u.getUsername() == null ? String.valueOf(u.getUserId()) : u.getUsername());
        LocalDateTime now = LocalDateTime.now();
        if (create) {
            w.setCreateBy(name);
            w.setCreateTime(now);
        }
        w.setUpdateBy(name);
        w.setUpdateTime(now);
    }

    public static String generateSecret() {
        // wh_ + 32 hex 字符,共 35 字节
        return "whsec_" + IdUtils.uuid();
    }

    public static String serializeEvents(List<String> events) {
        if (events == null) return "[]";
        List<String> filtered = events.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        return JsonUtils.toJson(filtered);
    }

    public static List<String> deserializeEvents(String json) {
        if (!StringUtils.hasText(json)) return Collections.emptyList();
        try {
            String[] arr = JsonUtils.fromJson(json, String[].class);
            if (arr == null) return Collections.emptyList();
            return Arrays.asList(arr);
        } catch (Exception e) {
            log.warn("反序列化 webhook events 失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public WebhookVO toVO(AiProjectWebhook w) {
        WebhookVO vo = new WebhookVO();
        vo.setId(w.getId());
        vo.setProjectId(w.getProjectId());
        vo.setName(w.getName());
        vo.setUrl(w.getUrl());
        vo.setEvents(deserializeEvents(w.getEvents()));
        vo.setStatus(w.getStatus());
        vo.setDescription(w.getDescription());
        vo.setCreateTime(w.getCreateTime());
        vo.setUpdateTime(w.getUpdateTime());
        return vo;
    }

    /**
     * 校验事件类型是否在白名单内,过滤异常值。
     */
    public static boolean isKnownEvent(String event) {
        if (event == null) return false;
        return WebhookEvent.FLOW_RUN_SUCCESS.equals(event)
                || WebhookEvent.FLOW_RUN_FAILED.equals(event)
                || WebhookEvent.ASSISTANT_CHAT_COMPLETED.equals(event)
                || WebhookEvent.KB_DOC_INDEXED.equals(event)
                || WebhookEvent.WEBHOOK_TEST.equals(event);
    }

    /**
     * 检查 webhook 是否订阅了某事件。
     */
    public static boolean subscribes(AiProjectWebhook w, String event) {
        List<String> evs = deserializeEvents(w.getEvents());
        return evs.contains(event) || evs.contains("*");
    }

    public List<AiProjectWebhook> findActiveSubscribers(Long projectId, String event) {
        return webhookMapper.selectActiveByProject(projectId).stream()
                .filter(w -> subscribes(w, event))
                .collect(Collectors.toList());
    }
}
