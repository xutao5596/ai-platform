package com.aiplatform.framework.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 业务指标统一入口(Sprint 5 Agent B)。
 * <p>
 * 静态方法在第一次调用时懒注册到 {@link MeterRegistry},Counter/Timer 实例按 tag 维度缓存,
 * 保证同一组 (name, tags) 只会产生一个 Meter,符合 Micrometer 的单例语义。
 * <p>
 * 命名规则:
 * <ul>
 *   <li>Counter: 业务事件计数,如 flow.run.count / api_key.call.count</li>
 *   <li>Timer:   业务耗时,如 flow.run.duration</li>
 * </ul>
 * Prometheus 端点会在应用启动后自动收集所有 Meter,无需额外代码。
 */
@Slf4j
public final class BusinessMetrics {

    /** Counter 单例缓存:key = metricName + "|" + tagKey + "=" + tagValue... */
    private static final ConcurrentMap<String, Counter> COUNTERS = new ConcurrentHashMap<>();

    /** Timer 单例缓存:key = metricName + "|" + tagKey + "=" + tagValue... */
    private static final ConcurrentMap<String, Timer> TIMERS = new ConcurrentHashMap<>();

    private static volatile MeterRegistry REGISTRY;

    private BusinessMetrics() {
    }

    /**
     * Spring 启动时注入全局 MeterRegistry(由 {@link MetricsConfig} 调用)。
     * 必须在任何业务指标触发前调用,否则首次 counter 仍会从 REGISTRY 兜底取,延迟注册。
     */
    public static void setRegistry(MeterRegistry registry) {
        REGISTRY = registry;
        log.info("BusinessMetrics 绑定 MeterRegistry: {}", registry.getClass().getSimpleName());
    }

    private static MeterRegistry registry() {
        MeterRegistry r = REGISTRY;
        if (r == null) {
            throw new IllegalStateException("MeterRegistry 未初始化,请确认 MetricsConfig 已注册");
        }
        return r;
    }

    private static Counter counter(String name, String description, Tags tags) {
        String key = name + "|" + tagsToKey(tags);
        return COUNTERS.computeIfAbsent(key, k -> Counter.builder(name)
                .description(description)
                .tags(tags)
                .register(registry()));
    }

    private static Timer timer(String name, String description, Tags tags) {
        String key = name + "|" + tagsToKey(tags);
        return TIMERS.computeIfAbsent(key, k -> Timer.builder(name)
                .description(description)
                .tags(tags)
                .register(registry()));
    }

    private static String tagsToKey(Tags tags) {
        if (tags == null) return "";
        StringBuilder sb = new StringBuilder();
        tags.forEach(t -> sb.append(t.getKey()).append('=').append(t.getValue()).append('|'));
        return sb.toString();
    }

    private static String safe(Object v) {
        return v == null ? "unknown" : Objects.toString(v);
    }

    private static String safeStatus(String status) {
        if (status == null || status.isBlank()) return "unknown";
        return status.toLowerCase();
    }

    // ---------------------------------------------------------------------
    // 流程执行 (FlowRunner 调用)
    // ---------------------------------------------------------------------

    /**
     * 流程开始:仅做"开始"计数,不做 Timer 标记(由 flowRunEnd 统一记耗时)。
     */
    public static void flowRunStart(Long flowId) {
        try {
            counter("flow.run.count", "流程执行次数",
                    Tags.of("flow_id", safe(flowId), "status", "running")).increment();
        } catch (Exception e) {
            log.debug("flowRunStart 指标注册失败: {}", e.getMessage());
        }
    }

    /**
     * 流程结束:按最终 status 计数 + 记录 Timer。
     */
    public static void flowRunEnd(Long flowId, String status, long costMs) {
        try {
            String st = safeStatus(status);
            Tags tags = Tags.of("flow_id", safe(flowId), "status", st);
            counter("flow.run.count", "流程执行次数", tags).increment();
            timer("flow.run.duration", "流程执行耗时", tags).record(Duration.ofMillis(Math.max(0L, costMs)));
        } catch (Exception e) {
            log.debug("flowRunEnd 指标注册失败: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------------------
    // 节点执行 (LlmNode / HttpNode 等调用)
    // ---------------------------------------------------------------------

    public static void flowNodeExecute(String nodeType, String status) {
        try {
            counter("flow.node.execute", "流程节点执行次数",
                    Tags.of("node_type", safe(nodeType), "status", safeStatus(status))).increment();
        } catch (Exception e) {
            log.debug("flowNodeExecute 指标注册失败: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------------------
    // API Key 调用 (ApiKeyAuthFilter 调用)
    // ---------------------------------------------------------------------

    public static void apiKeyCall(Long apiKeyId, String path, String status) {
        try {
            counter("api_key.call.count", "API Key 调用次数",
                    Tags.of("api_key_id", safe(apiKeyId),
                            "path", safe(path),
                            "status", safeStatus(status))).increment();
        } catch (Exception e) {
            log.debug("apiKeyCall 指标注册失败: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------------------
    // Webhook 派发 (WebhookDispatcher 调用)
    // ---------------------------------------------------------------------

    public static void webhookDispatch(Long webhookId, String event, String status) {
        try {
            counter("webhook.dispatch.count", "Webhook 派发次数",
                    Tags.of("webhook_id", safe(webhookId),
                            "event", safe(event),
                            "status", safeStatus(status))).increment();
        } catch (Exception e) {
            log.debug("webhookDispatch 指标注册失败: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------------------
    // 登录 (AuthService.login 调用)
    // ---------------------------------------------------------------------

    public static void login(String status) {
        try {
            counter("login.count", "登录次数",
                    Tags.of("status", safeStatus(status))).increment();
        } catch (Exception e) {
            log.debug("login 指标注册失败: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------------------
    // 助手对话 (AssistantChatService 调用)
    // ---------------------------------------------------------------------

    public static void assistantChat(Long assistantId, String status) {
        try {
            counter("assistant.chat.count", "助手对话次数",
                    Tags.of("assistant_id", safe(assistantId),
                            "status", safeStatus(status))).increment();
        } catch (Exception e) {
            log.debug("assistantChat 指标注册失败: {}", e.getMessage());
        }
    }
}
