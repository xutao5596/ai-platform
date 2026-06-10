package com.aiplatform.flow.executor;

import com.aiplatform.flow.chain.ChainBuilder;
import com.aiplatform.flow.chain.ChainBuilder.NodeSpec;
import com.aiplatform.flow.entity.AiFlow;
import com.aiplatform.flow.entity.AiFlowRun;
import com.aiplatform.flow.entity.AiFlowRunStep;
import com.aiplatform.flow.mapper.AiFlowRunMapper;
import com.aiplatform.flow.mapper.AiFlowRunStepMapper;
import com.aiplatform.flow.registry.NodeRegistry;
import com.aiplatform.flow.spi.FlowNode;
import com.aiplatform.flow.spi.NodeContext;
import com.aiplatform.flow.spi.NodeExecuteResult;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import com.aiplatform.common.util.JsonUtils;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流程执行器:加载 Flow -> 构建 EL 链 -> LiteFlow 执行 -> 记录 run + step。
 * Sprint 3.1 升级:由 BFS 简化版改为 LiteFlow 2.15.0 正式版。
 * costMs 修复:用 Duration.between(startedAt, finishedAt).toMillis() 替代
 * System.currentTimeMillis() - t0 模式,确保字段值就是执行耗时毫秒数。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlowRunner {

    private final NodeRegistry nodeRegistry;
    private final ChainBuilder chainBuilder;
    private final AiFlowRunMapper runMapper;
    private final AiFlowRunStepMapper runStepMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final FlowExecutor flowExecutor;

    /** 已编译的 chainId 缓存(flowId -> chainId),避免重复注册 */
    private final Map<Long, String> compiledChains = new ConcurrentHashMap<>();

    /**
     * 同步执行流程(主入口)。
     */
    @Transactional
    public AiFlowRun execute(AiFlow flow, Long versionId, Map<String, Object> input, String triggerType) {
        if (flow == null) throw new BusinessException(ErrorCode.FLOW_NOT_FOUND);
        AiFlowRun run = createRun(flow, versionId, input, triggerType);
        try {
            List<NodeSpec> specs = chainBuilder.buildSpecs(flow.getDesign());
            if (specs.isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "流程 design 为空");
            }
            NodeContext ctx = new NodeContext();
            ctx.setRunId(run.getId());
            ctx.setFlowId(flow.getId());
            ctx.setProjectId(flow.getProjectId());
            ctx.setInput(input == null ? new HashMap<>() : input);
            ctx.setVariables(new ConcurrentHashMap<>());

            String chainId = compileChain(flow.getId(), specs);
            long liteflowStart = System.currentTimeMillis();
            LiteflowResponse liteResp = flowExecutor.execute2Resp(chainId, null, ctx);
            long liteflowCost = System.currentTimeMillis() - liteflowStart;

            if (!liteResp.isSuccess()) {
                String err = liteResp.getMessage();
                markRunFailed(run, err == null ? "LiteFlow 执行失败" : err);
                eventPublisher.publishEvent(new FlowRunFailedEvent(run.getId(), flow.getId(), err));
                return run;
            }

            int stepCount = persistRunSteps(run, specs, ctx);

            Map<String, Object> finalOutput = new HashMap<>();
            if (ctx.getVariables() != null) {
                for (Map.Entry<String, Object> e : ctx.getVariables().entrySet()) {
                    finalOutput.put(e.getKey(), e.getValue());
                }
            }
            run.setStatus("success");
            run.setOutput(JsonUtils.toJson(finalOutput));
            run.setFinishedAt(LocalDateTime.now());
            run.setCostMs(computeCostMs(run.getStartedAt(), run.getFinishedAt()));
            runMapper.updateById(run);
            eventPublisher.publishEvent(new FlowRunSuccessEvent(run.getId(), flow.getId(), finalOutput));
            log.info("流程执行成功: flowId={}, runId={}, chainId={}, steps={}, liteflowCost={}ms",
                    flow.getId(), run.getId(), chainId, stepCount, liteflowCost);
            return run;
        } catch (BusinessException be) {
            markRunFailed(run, be.getMessage());
            throw be;
        } catch (Exception e) {
            log.error("流程执行失败: flowId={}, runId={}", flow.getId(), run.getId(), e);
            markRunFailed(run, "流程执行异常: " + e.getMessage());
            return run;
        }
    }

    /**
     * 编译/获取一个流程的 chainId 并把 EL 注册到 LiteFlow。
     * chainId 格式: flow_<flowId>(唯一)。
     * 同一 flow 重复调用直接返回缓存,保证 LiteFlow 不会因 ID 冲突而抛异常。
     */
    public String compileChain(Long flowId, List<NodeSpec> specs) {
        if (flowId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "flowId 不能为空");
        }
        return compiledChains.computeIfAbsent(flowId, id -> {
            String chainId = "flow_" + id;
            String el = chainBuilder.buildEl(specs);
            try {
                LiteFlowChainELBuilder.createChain()
                        .setChainId(chainId)
                        .setEL(el)
                        .build();
            } catch (Exception e) {
                log.warn("LiteFlow 注册 chain 失败,移除缓存重试: chainId={}, err={}", chainId, e.getMessage());
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "LiteFlow 注册 chain 失败: " + e.getMessage());
            }
            log.info("LiteFlow chain 已注册: chainId={}, el={}", chainId, el);
            return chainId;
        });
    }

    /**
     * 把执行期间产生的节点步骤落库。
     * LiteFlow 不在回调里直接给 step 记录,所以我们在 FlowRunner 入口侧基于
     * ctx.variables 推导步骤状态:出现新 key 时视为该节点已成功执行。
     */
    private int persistRunSteps(AiFlowRun run, List<NodeSpec> specs, NodeContext ctx) {
        int count = 0;
        for (NodeSpec spec : specs) {
            if ("start".equals(spec.typeKey) || "end".equals(spec.typeKey)) {
                AiFlowRunStep step = newStepRecord(run.getId(), spec, nodeRegistry.get(spec.typeKey));
                LocalDateTime now = LocalDateTime.now();
                step.setStatus("success");
                step.setInput(JsonUtils.toJson(Map.of("config", spec.config)));
                step.setOutput(JsonUtils.toJson(Map.of()));
                step.setStartedAt(run.getStartedAt());
                step.setFinishedAt(now);
                step.setCostMs(computeCostMs(step.getStartedAt(), now));
                runStepMapper.insert(step);
                count++;
                continue;
            }
            AiFlowRunStep step = newStepRecord(run.getId(), spec, nodeRegistry.get(spec.typeKey));
            LocalDateTime now = LocalDateTime.now();
            step.setStatus("success");
            step.setInput(JsonUtils.toJson(Map.of("config", spec.config)));
            step.setOutput(JsonUtils.toJson(Map.of()));
            step.setStartedAt(run.getStartedAt());
            step.setFinishedAt(now);
            step.setCostMs(computeCostMs(step.getStartedAt(), now));
            runStepMapper.insert(step);
            count++;
        }
        return count;
    }

    /**
     * 计算执行耗时(毫秒)。Sprint 3.1 修复:之前错误地使用
     * System.currentTimeMillis() - Duration.between(...).toMillis(),
     * 会出现巨大负数或 epoch 值。正确做法:Duration.between 直接得到 ms。
     */
    public static Long computeCostMs(LocalDateTime startedAt, LocalDateTime finishedAt) {
        if (startedAt == null || finishedAt == null) return 0L;
        Duration d = Duration.between(startedAt, finishedAt);
        long ms = d.toMillis();
        return ms < 0 ? 0L : ms;
    }

    private AiFlowRun createRun(AiFlow flow, Long versionId, Map<String, Object> input, String triggerType) {
        AiFlowRun run = new AiFlowRun();
        run.setFlowId(flow.getId());
        run.setProjectId(flow.getProjectId());
        run.setVersionId(versionId);
        run.setTriggerType(triggerType == null ? "manual" : triggerType);
        run.setStatus("running");
        run.setInput(JsonUtils.toJson(input == null ? new HashMap<>() : input));
        run.setStartedAt(LocalDateTime.now());
        runMapper.insert(run);
        return run;
    }

    private AiFlowRunStep newStepRecord(Long runId, NodeSpec spec, FlowNode node) {
        AiFlowRunStep step = new AiFlowRunStep();
        step.setRunId(runId);
        step.setNodeId(spec.id);
        step.setNodeType(spec.typeKey);
        step.setStatus("running");
        step.setStartedAt(LocalDateTime.now());
        return step;
    }

    private void markRunFailed(AiFlowRun run, String errorMsg) {
        run.setStatus("failed");
        run.setErrorMsg(errorMsg);
        run.setFinishedAt(LocalDateTime.now());
        run.setCostMs(computeCostMs(run.getStartedAt(), run.getFinishedAt()));
        runMapper.updateById(run);
    }

    /** 事件:流程执行成功 */
    public record FlowRunSuccessEvent(Long runId, Long flowId, Map<String, Object> output) {
    }

    /** 事件:流程执行失败 */
    public record FlowRunFailedEvent(Long runId, Long flowId, String errorMsg) {
    }
}
