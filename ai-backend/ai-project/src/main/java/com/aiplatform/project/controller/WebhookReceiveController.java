package com.aiplatform.project.controller;

import com.aiplatform.common.api.Result;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import com.aiplatform.common.util.JsonUtils;
import com.aiplatform.project.entity.AiProjectWebhook;
import com.aiplatform.project.entity.AiProjectWebhookLog;
import com.aiplatform.project.mapper.AiProjectWebhookLogMapper;
import com.aiplatform.project.service.WebhookDispatcher;
import com.aiplatform.project.service.WebhookService;
import com.aiplatform.common.util.IdUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 外部系统调用入口:POST /api/v1/webhook/receive/{id}
 *
 * 匿名访问(JwtAuthenticationFilter 已放行 /api/v1/webhook/**),
 * 验证 HMAC 签名 + 5 分钟时间窗口,然后记录投递日志(入站方向),并按事件类型
 * 触发 WebhookDispatcher 异步分发(出站方向)给该项目内所有订阅了该事件的 webhook。
 *
 * 签名协议:
 *   X-Webhook-Signature: sha256=&lt;HMAC-SHA256(secret, rawBody)&gt;
 *   X-Webhook-Timestamp: &lt;unix-seconds&gt;
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/webhook/receive")
@RequiredArgsConstructor
public class WebhookReceiveController {

    /** 时间戳有效期(秒) */
    private static final long TIMESTAMP_TOLERANCE_SEC = 300L;

    private final WebhookService webhookService;
    private final WebhookDispatcher dispatcher;
    private final AiProjectWebhookLogMapper logMapper;

    @PostMapping("/{id}")
    public Result<Map<String, Object>> receive(@PathVariable("id") Long id,
                                               @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
                                               @RequestHeader(value = "X-Webhook-Timestamp", required = false) String timestamp,
                                               @RequestHeader(value = "X-Webhook-Event", required = false) String event,
                                               @RequestBody(required = false) String rawBody) {
        if (id == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "webhook id 不能为空");
        }
        AiProjectWebhook w = webhookService.mustGet(id, null); // 接收端无 projectId 上下文,绕过项目归属校验
        if (w.getStatus() == null || w.getStatus() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "webhook 已禁用");
        }
        if (rawBody == null) rawBody = "";

        // 1. 时间戳验证(防重放)
        long now = Instant.now().getEpochSecond();
        long ts;
        try {
            ts = Long.parseLong(timestamp == null ? "0" : timestamp.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "X-Webhook-Timestamp 格式错误");
        }
        if (Math.abs(now - ts) > TIMESTAMP_TOLERANCE_SEC) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "时间戳超出 5 分钟窗口");
        }

        // 2. 签名验证
        if (!WebhookDispatcher.verifySignature(w.getSecret(), rawBody, signature)) {
            log.info("Webhook 入站签名校验失败: webhookId={}, ts={}", id, ts);
            throw new BusinessException(ErrorCode.FORBIDDEN, "签名验证失败");
        }

        // 3. 解析 body
        Map<String, Object> inboundPayload;
        if (rawBody.isEmpty()) {
            inboundPayload = new HashMap<>();
        } else {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = JsonUtils.fromJson(rawBody, Map.class);
                inboundPayload = parsed == null ? new HashMap<>() : parsed;
            } catch (Exception e) {
                log.info("Webhook 入站 body 不是 JSON,作为原始字符串透传: webhookId={}", id);
                inboundPayload = new HashMap<>();
                inboundPayload.put("raw", rawBody);
            }
        }

        // 4. 事件类型:未指定则使用 webhook.received
        String eventType = (event != null && !event.isBlank()) ? event : "webhook.received";
        if (!WebhookService.isKnownEvent(eventType) && !"webhook.received".equals(eventType)) {
            // 未知事件也允许通过,作为 webhook.received 处理(向后兼容)
            log.debug("Webhook 入站未识别事件类型: {}", eventType);
        }

        // 5. 记录入站日志(200 OK)
        saveInboundLog(w, eventType, rawBody, 200, "accepted");

        // 6. 异步触发出站分发(给该项目订阅了该事件的所有 webhook)
        Map<String, Object> outboundPayload = new HashMap<>();
        outboundPayload.put("source", "inbound");
        outboundPayload.put("sourceWebhookId", w.getId());
        outboundPayload.put("sourceEvent", eventType);
        if (inboundPayload != null) outboundPayload.putAll(inboundPayload);
        try {
            dispatcher.dispatch(w.getProjectId(), eventType, outboundPayload);
        } catch (Exception e) {
            log.warn("Webhook 出站分发失败: webhookId={}, event={}", id, eventType, e);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("ok", true);
        resp.put("event", eventType);
        resp.put("webhookId", id);
        resp.put("acceptedAt", System.currentTimeMillis());
        return Result.ok(resp);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveInboundLog(AiProjectWebhook w, String eventType, String rawBody, int status, String msg) {
        try {
            AiProjectWebhookLog row = new AiProjectWebhookLog();
            row.setWebhookId(w.getId());
            row.setProjectId(w.getProjectId());
            row.setEvent("IN:" + eventType);
            row.setRequestUrl("INBOUND");
            row.setResponseStatus(status);
            row.setResponseBody(msg);
            row.setRequestPayload(truncate(rawBody, 4000));
            row.setRetryCount(0);
            row.setCostMs(0L);
            row.setCreateTime(LocalDateTime.now());
            logMapper.insert(row);
        } catch (Exception e) {
            log.warn("保存入站日志失败: webhookId={}, err={}", w.getId(), e.getMessage());
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
