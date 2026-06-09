package com.aiplatform.flow.trigger;

import com.aiplatform.flow.entity.AiFlowTrigger;
import com.aiplatform.flow.mapper.AiFlowTriggerMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Cron 触发器:基于 Spring TaskScheduler。Sprint 3.1 替换为 Quartz。
 */
@Slf4j
@Component
public class CronTrigger implements FlowTrigger {

    private final TaskScheduler taskScheduler;
    private final AiFlowTriggerMapper triggerMapper;
    private final TriggerRegistry registry;

    public CronTrigger(TaskScheduler taskScheduler, AiFlowTriggerMapper triggerMapper, @Lazy TriggerRegistry registry) {
        this.taskScheduler = taskScheduler;
        this.triggerMapper = triggerMapper;
        this.registry = registry;
    }

    private final Map<Long, ScheduledFuture<?>> schedules = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        reloadAll();
        log.info("CronTrigger 启动完成,当前调度数: {}", schedules.size());
    }

    @PreDestroy
    public void shutdown() {
        schedules.values().forEach(f -> f.cancel(false));
        schedules.clear();
    }

    @Override
    public String getType() {
        return "cron";
    }

    @Override
    public void trigger(Long flowId, Map<String, Object> input) {
        log.debug("CronTrigger.trigger called: flowId={}", flowId);
    }

    public synchronized void reloadAll() {
        schedules.values().forEach(f -> f.cancel(false));
        schedules.clear();
        try {
            List<AiFlowTrigger> list = triggerMapper.selectList(
                    new LambdaQueryWrapper<AiFlowTrigger>()
                            .eq(AiFlowTrigger::getType, "cron")
                            .eq(AiFlowTrigger::getStatus, 1));
            for (AiFlowTrigger t : list) {
                schedule(t);
            }
        } catch (Exception e) {
            log.warn("加载 cron 任务失败: {}", e.getMessage());
        }
    }

    public synchronized void schedule(AiFlowTrigger t) {
        cancel(t.getId());
        if (t.getStatus() == null || t.getStatus() != 1) return;
        String cron = parseCronExpression(t.getConfig());
        if (cron == null) {
            log.warn("CronTrigger id={} 配置无效: config={}", t.getId(), t.getConfig());
            return;
        }
        try {
            org.springframework.scheduling.support.CronTrigger cronTrigger =
                    new org.springframework.scheduling.support.CronTrigger(cron);
            ScheduledFuture<?> f = taskScheduler.schedule(
                    () -> {
                        try {
                            Map<String, Object> input = parseInput(t.getConfig());
                            registry.fire("cron", t.getFlowId(), input);
                        } catch (Exception e) {
                            log.warn("Cron 任务执行失败: triggerId={}", t.getId(), e);
                        }
                    },
                    cronTrigger);
            schedules.put(t.getId(), f);
            log.info("Cron 任务已注册: triggerId={}, flowId={}, cron={}", t.getId(), t.getFlowId(), cron);
        } catch (Exception e) {
            log.warn("Cron 表达式无效: triggerId={}, cron={}", t.getId(), cron, e);
        }
    }

    public synchronized void cancel(Long triggerId) {
        ScheduledFuture<?> f = schedules.remove(triggerId);
        if (f != null) f.cancel(false);
    }

    private String parseCronExpression(String configJson) {
        if (configJson == null || configJson.isBlank()) return null;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> cfg = com.aiplatform.common.util.JsonUtils.fromJson(configJson, Map.class);
            if (cfg == null) return null;
            Object cron = cfg.get("cron");
            return cron == null ? null : String.valueOf(cron);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseInput(String configJson) {
        if (configJson == null || configJson.isBlank()) return new HashMap<>();
        try {
            Map<String, Object> cfg = com.aiplatform.common.util.JsonUtils.fromJson(configJson, Map.class);
            if (cfg == null) return new HashMap<>();
            Object input = cfg.get("input");
            return input instanceof Map ? (Map<String, Object>) input : new HashMap<>();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
