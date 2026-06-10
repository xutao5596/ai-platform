package com.aiplatform.project.service;

import com.aiplatform.common.util.IdUtils;
import com.aiplatform.common.util.JsonUtils;
import com.aiplatform.framework.observability.BusinessMetrics;
import com.aiplatform.project.entity.AiProjectWebhook;
import com.aiplatform.project.entity.AiProjectWebhookLog;
import com.aiplatform.project.mapper.AiProjectWebhookLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Webhook 异步分发器:
 * 1. 接收到事件后,查该项目所有 active 且订阅了该事件的 webhook;
 * 2. 计算 HMAC-SHA256(secret, body) 作为签名;
 * 3. HTTP POST 目标 URL,记录 ai_project_webhook_log;
 * 4. 失败按 1s / 5s / 30s / 5min 退避重试,最多 4 次。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookDispatcher {

    /** 重试退避(秒) */
    private static final long[] RETRY_DELAYS_SEC = {1L, 5L, 30L, 300L};
    /** HTTP 超时(秒) */
    private static final int HTTP_TIMEOUT_SEC = 10;
    /** 用户代理 */
    private static final String USER_AGENT = "AI-Platform-Webhook/1.0";

    private final WebhookService webhookService;
    private final AiProjectWebhookLogMapper logMapper;
    private final TaskScheduler taskScheduler;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    /** 最大尝试次数(含首次)。共 5 次:首次 + 4 次重试。 */
    private static final int MAX_ATTEMPTS = RETRY_DELAYS_SEC.length + 1;

    /**
     * 异步入口:事件触发后调此方法,projectId 必填。
     */
    @Async
    public void dispatch(Long projectId, String eventType, Map<String, Object> payload) {
        if (projectId == null || eventType == null) return;
        try {
            List<AiProjectWebhook> subs = webhookService.findActiveSubscribers(projectId, eventType);
            if (subs.isEmpty()) {
                log.debug("Webhook dispatch: no subscribers for project={}, event={}", projectId, eventType);
                return;
            }
            for (AiProjectWebhook w : subs) {
                Map<String, Object> body = new java.util.HashMap<>();
                body.put("id", IdUtils.uuid());
                body.put("event", eventType);
                body.put("projectId", projectId);
                body.put("deliveredAt", System.currentTimeMillis() / 1000L);
                if (payload != null) body.putAll(payload);
                // attempt = 1 表示首次
                sendWithRetry(w, eventType, body, 1);
            }
        } catch (Exception e) {
            log.warn("Webhook dispatch failed: project={}, event={}", projectId, eventType, e);
        }
    }

    /**
     * 同步发送(被测试端点使用,直接发一次不重试)。
     */
    public boolean sendOnce(AiProjectWebhook w, String eventType, Map<String, Object> body) {
        return doSend(w, eventType, body, 1);
    }

    /**
     * 重试调度核心:递归安排下一次尝试。
     * @param attempt 当前尝试序号(1 = 首次,2 = 第 1 次重试,...,MAX_ATTEMPTS = 最后一次)
     */
    private void sendWithRetry(AiProjectWebhook w, String eventType, Map<String, Object> body, int attempt) {
        boolean ok = doSend(w, eventType, body, attempt);
        if (ok || attempt >= MAX_ATTEMPTS) {
            if (!ok) {
                log.warn("Webhook 重试耗尽(共 {} 次): webhookId={}, event={}", attempt, w.getId(), eventType);
            }
            return;
        }
        long delay = RETRY_DELAYS_SEC[attempt - 1];
        Instant fireAt = Instant.now().plusSeconds(delay);
        log.info("Webhook 重试安排: webhookId={}, event={}, nextAttempt={}, delay={}s",
                w.getId(), eventType, attempt + 1, delay);
        taskScheduler.schedule(() -> sendWithRetry(w, eventType, body, attempt + 1), fireAt);
    }

    /**
     * 实际发送 + 写日志。
     * @param attempt 第几次(0 = 首次)
     * @return 是否成功
     */
    private boolean doSend(AiProjectWebhook w, String eventType, Map<String, Object> body, int attempt) {
        AiProjectWebhookLog logRow = new AiProjectWebhookLog();
        logRow.setWebhookId(w.getId());
        logRow.setProjectId(w.getProjectId());
        logRow.setEvent(eventType);
        logRow.setRequestUrl(w.getUrl());
        logRow.setRetryCount(attempt);
        logRow.setCreateTime(LocalDateTime.now());

        String payloadJson = JsonUtils.toJson(body);
        logRow.setRequestPayload(truncate(payloadJson, 4000));

        long start = System.currentTimeMillis();
        try {
            String signature = hmacSha256Hex(w.getSecret(), payloadJson);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(w.getUrl()))
                    .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SEC))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("User-Agent", USER_AGENT)
                    .header("X-Webhook-Event", eventType)
                    .header("X-Webhook-Signature", "sha256=" + signature)
                    .header("X-Webhook-Timestamp", String.valueOf(System.currentTimeMillis() / 1000L))
                    .header("X-Webhook-Id", String.valueOf(body.get("id")))
                    .POST(HttpRequest.BodyPublishers.ofString(payloadJson, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = resp.statusCode();
            long cost = System.currentTimeMillis() - start;
            logRow.setResponseStatus(status);
            logRow.setResponseBody(truncate(resp.body(), 4000));
            logRow.setCostMs(cost);
            saveLog(logRow);
            boolean ok = status >= 200 && status < 300;
            if (!ok) {
                log.info("Webhook 投递失败: webhookId={}, status={}, cost={}ms", w.getId(), status, cost);
            }
            BusinessMetrics.webhookDispatch(w.getId(), eventType, ok ? "success" : "failed");
            return ok;
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            logRow.setResponseStatus(-1);
            logRow.setResponseBody("EXCEPTION: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            logRow.setCostMs(cost);
            saveLog(logRow);
            log.info("Webhook 投递异常: webhookId={}, cost={}ms, err={}", w.getId(), cost, e.getMessage());
            BusinessMetrics.webhookDispatch(w.getId(), eventType, "exception");
            return false;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLog(AiProjectWebhookLog row) {
        try {
            logMapper.insert(row);
        } catch (Exception e) {
            log.warn("保存 webhook 日志失败: webhookId={}, err={}", row.getWebhookId(), e.getMessage());
        }
    }

    /**
     * HMAC-SHA256(secret, body) -> hex。
     */
    public static String hmacSha256Hex(String secret, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sig = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(sig);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 计算失败", e);
        }
    }

    /**
     * 校验签名(用于 WebhookReceiveController 入站验签)。
     * @param providedHeader 形如 "sha256=abcdef..."
     */
    public static boolean verifySignature(String secret, String body, String providedHeader) {
        if (providedHeader == null) return false;
        String expected = "sha256=" + hmacSha256Hex(secret, body);
        return constantTimeEquals(expected, providedHeader);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
