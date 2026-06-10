package com.aiplatform.project.controller;

import com.aiplatform.common.api.PageResult;
import com.aiplatform.common.api.Result;
import com.aiplatform.common.context.UserContext;
import com.aiplatform.project.annotation.PreProjectRole;
import com.aiplatform.project.dto.WebhookSaveRequest;
import com.aiplatform.project.dto.WebhookTestRequest;
import com.aiplatform.project.dto.WebhookVO;
import com.aiplatform.project.entity.AiProjectWebhook;
import com.aiplatform.project.entity.AiProjectWebhookLog;
import com.aiplatform.project.mapper.AiProjectWebhookLogMapper;
import com.aiplatform.project.service.WebhookDispatcher;
import com.aiplatform.project.service.WebhookService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Webhook 管理端点(JWT 鉴权,@PreProjectRole 控制项目内权限)。
 * 接收端点见 {@link WebhookReceiveController}(匿名,签名验证)。
 */
@RestController
@RequestMapping("/api/v1/project/{projectId}/webhooks")
@RequiredArgsConstructor
public class ProjectWebhookController {

    private final WebhookService webhookService;
    private final WebhookDispatcher dispatcher;
    private final AiProjectWebhookLogMapper logMapper;

    @GetMapping
    @PreProjectRole
    public Result<List<WebhookVO>> list(@PathVariable("projectId") Long projectId) {
        return Result.ok(webhookService.list(projectId));
    }

    @GetMapping("/{id}")
    @PreProjectRole
    public Result<WebhookVO> get(@PathVariable("projectId") Long projectId, @PathVariable("id") Long id) {
        return Result.ok(webhookService.get(projectId, id));
    }

    @PostMapping
    @PreProjectRole("admin")
    public Result<WebhookVO> create(@PathVariable("projectId") Long projectId,
                                    @RequestBody @Valid WebhookSaveRequest req) {
        return Result.ok("创建成功(secret 仅此一次返回)", webhookService.create(projectId, req));
    }

    @PutMapping("/{id}")
    @PreProjectRole("admin")
    public Result<WebhookVO> update(@PathVariable("projectId") Long projectId,
                                    @PathVariable("id") Long id,
                                    @RequestBody @Valid WebhookSaveRequest req) {
        req.setId(id);
        return Result.ok(webhookService.update(projectId, req));
    }

    @DeleteMapping("/{id}")
    @PreProjectRole("admin")
    public Result<Void> delete(@PathVariable("projectId") Long projectId, @PathVariable("id") Long id) {
        webhookService.delete(projectId, id);
        return Result.ok();
    }

    /**
     * 重置 secret:生成新的 secret 并返回明文。
     */
    @PostMapping("/{id}/reset-secret")
    @PreProjectRole("admin")
    public Result<WebhookVO> resetSecret(@PathVariable("projectId") Long projectId,
                                         @PathVariable("id") Long id) {
        return Result.ok("secret 已重置(明文仅此一次返回)", webhookService.resetSecret(projectId, id));
    }

    /**
     * 测试发送:立刻发一个测试 payload(不重试),返回发送结果。
     */
    @PostMapping("/{id}/test")
    @PreProjectRole("admin")
    public Result<Map<String, Object>> test(@PathVariable("projectId") Long projectId,
                                            @PathVariable("id") Long id,
                                            @RequestBody(required = false) WebhookTestRequest req) {
        AiProjectWebhook w = webhookService.mustGet(id, projectId);
        String event = (req != null && req.getEvent() != null && !req.getEvent().isBlank())
                ? req.getEvent() : "webhook.test";

        Map<String, Object> body = new HashMap<>();
        body.put("test", true);
        body.put("from", "manual-test");
        body.put("userId", UserContext.getUserId());
        if (req != null && req.getPayload() != null) {
            body.putAll(req.getPayload());
        }

        boolean ok = dispatcher.sendOnce(w, event, body);
        Map<String, Object> data = new HashMap<>();
        data.put("ok", ok);
        data.put("event", event);
        data.put("message", ok ? "已发送,等待目标响应" : "发送失败,请查看投递日志");
        return Result.ok(data);
    }

    /**
     * 投递日志分页查询(按 webhookId 倒序)。
     */
    @GetMapping("/{id}/logs")
    @PreProjectRole
    public Result<PageResult<AiProjectWebhookLog>> logs(@PathVariable("projectId") Long projectId,
                                                       @PathVariable("id") Long id,
                                                       @RequestParam(defaultValue = "1") long current,
                                                       @RequestParam(defaultValue = "20") long size) {
        // 校验 webhook 存在且属于该项目
        webhookService.mustGet(id, projectId);
        Page<AiProjectWebhookLog> page = new Page<>(current, size);
        return Result.ok(PageResult.of(logMapper.selectPageByWebhook(page, id)));
    }
}
