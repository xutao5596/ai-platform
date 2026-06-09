package com.aiplatform.flow.trigger;

import com.aiplatform.flow.entity.AiFlow;
import com.aiplatform.flow.executor.FlowRunner;
import com.aiplatform.flow.mapper.AiFlowMapper;
import com.aiplatform.flow.mapper.AiFlowTriggerMapper;
import com.aiplatform.flow.entity.AiFlowTrigger;
import com.aiplatform.common.context.LoginUser;
import com.aiplatform.common.context.UserContext;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 触发器注册中心:按 type 索引 FlowTrigger 实现。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TriggerRegistry {

    private final ApplicationContext applicationContext;
    private final AiFlowMapper flowMapper;
    private final AiFlowTriggerMapper triggerMapper;
    private final FlowRunner flowExecutor;

    private final Map<String, FlowTrigger> triggers = new HashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    private List<FlowTrigger> triggerBeans;

    @jakarta.annotation.PostConstruct
    public void init() {
        for (FlowTrigger t : triggerBeans) {
            triggers.put(t.getType(), t);
        }
        log.info("TriggerRegistry 初始化完成: {}", triggers.keySet());
    }

    public FlowTrigger get(String type) {
        return triggers.get(type);
    }

    public List<FlowTrigger> all() {
        return List.copyOf(triggers.values());
    }

    /**
     * 通用 fire:按 triggerType 查对应的 trigger 实现,
     * 内部异步执行 flow(flowId, input)。
     */
    public void fire(String triggerType, Long flowId, Map<String, Object> input) {
        FlowTrigger t = triggers.get(triggerType);
        if (t == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的触发器类型: " + triggerType);
        }
        // 验证 flow 存在
        AiFlow f = flowMapper.selectById(flowId);
        if (f == null) {
            throw new BusinessException(ErrorCode.FLOW_NOT_FOUND);
        }
        // 更新 trigger last_run
        updateTriggerLastRun(flowId, triggerType);

        // 在新线程中执行(异步)
        Thread exec = new Thread(() -> {
            LoginUser prev = null;
            try {
                // 异步线程:把当前 user 传递过去(若有)
                LoginUser cur = UserContext.get();
                if (cur != null) {
                    prev = cur;
                    UserContext.set(cur);
                }
                flowExecutor.execute(f, null, input, triggerType);
            } catch (Exception e) {
                log.warn("异步触发器执行失败: type={}, flowId={}", triggerType, flowId, e);
            } finally {
                if (prev != null) UserContext.set(prev);
            }
        }, "flow-trigger-" + triggerType + "-" + flowId);
        exec.setDaemon(true);
        exec.start();
    }

    /** 同步执行 — 用于 ManualTrigger(Web 端点需要即时返回) */
    public com.aiplatform.flow.entity.AiFlowRun executeSync(Long flowId, Map<String, Object> input, String triggerType) {
        AiFlow f = flowMapper.selectById(flowId);
        if (f == null) {
            throw new BusinessException(ErrorCode.FLOW_NOT_FOUND);
        }
        updateTriggerLastRun(flowId, triggerType);
        return flowExecutor.execute(f, null, input, triggerType);
    }

    private void updateTriggerLastRun(Long flowId, String triggerType) {
        try {
            AiFlowTrigger t = triggerMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiFlowTrigger>()
                            .eq(AiFlowTrigger::getFlowId, flowId)
                            .eq(AiFlowTrigger::getType, triggerType)
                            .last("LIMIT 1"));
            if (t != null) {
                t.setLastRunAt(java.time.LocalDateTime.now());
                triggerMapper.updateById(t);
            }
        } catch (Exception ignore) {
        }
    }
}
