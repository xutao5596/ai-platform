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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程执行器:加载 Flow -> 构建 NodeSpec 链 -> 顺序执行每个节点 -> 记录 run + step。
 * 简化实现:基于 BFS 顺次调用节点(非完整 LiteFlow 集成,Sprint 3.1 完善)。
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
            ctx.setVariables(new HashMap<>());

            NodeSpec start = specs.stream()
                    .filter(s -> "start".equals(s.typeKey))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "流程缺少 start 节点"));

            java.util.LinkedList<String> queue = new java.util.LinkedList<>();
            queue.add(start.id);
            java.util.Set<String> visited = new java.util.HashSet<>();
            int nodeCount = 0;
            while (!queue.isEmpty()) {
                String curId = queue.poll();
                if (visited.contains(curId)) continue;
                visited.add(curId);
                NodeSpec spec = specs.stream().filter(s -> s.id.equals(curId)).findFirst().orElse(null);
                if (spec == null) continue;
                FlowNode node = nodeRegistry.get(spec.typeKey);
                if (node == null) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST,
                            "未注册节点类型: " + spec.typeKey);
                }
                ctx.setNodeId(spec.id);
                ctx.setNodeType(spec.typeKey);
                ctx.setNodeConfig(spec.config);
                long t0 = System.currentTimeMillis();
                AiFlowRunStep step = newStepRecord(run.getId(), spec, node);
                NodeExecuteResult result;
                try {
                    result = node.execute(ctx);
                } catch (Exception e) {
                    log.error("节点执行异常: flow={}, node={}, type={}", flow.getId(), spec.id, spec.typeKey, e);
                    result = NodeExecuteResult.fail("节点执行异常: " + e.getMessage());
                }
                long cost = System.currentTimeMillis() - t0;
                step.setCostMs(cost);
                step.setStatus(Boolean.TRUE.equals(result.isSuccess()) ? "success" : "failed");
                step.setInput(JsonUtils.toJson(buildStepInput(spec, ctx)));
                step.setOutput(JsonUtils.toJson(result.getOutput()));
                step.setFinishedAt(LocalDateTime.now());
                runStepMapper.insert(step);

                if (!result.isSuccess()) {
                    markRunFailed(run, "节点 " + spec.id + " 失败: " + result.getErrorMsg());
                    eventPublisher.publishEvent(new FlowRunFailedEvent(run.getId(), flow.getId(), result.getErrorMsg()));
                    return run;
                }
                if (result.getOutput() != null) {
                    for (Map.Entry<String, Object> e : result.getOutput().entrySet()) {
                        ctx.putVariable(e.getKey(), e.getValue());
                    }
                }
                if ("end".equals(spec.typeKey)) {
                    break;
                }
                queue.addAll(spec.next);
                nodeCount++;
            }
            Map<String, Object> finalOutput = new HashMap<>();
            for (Map.Entry<String, Object> e : ctx.getVariables().entrySet()) {
                finalOutput.put(e.getKey(), e.getValue());
            }
            run.setStatus("success");
            run.setOutput(JsonUtils.toJson(finalOutput));
            run.setFinishedAt(LocalDateTime.now());
            if (run.getStartedAt() != null) {
                run.setCostMs(System.currentTimeMillis() - java.time.Duration.between(run.getStartedAt(), run.getFinishedAt()).toMillis());
            }
            runMapper.updateById(run);
            eventPublisher.publishEvent(new FlowRunSuccessEvent(run.getId(), flow.getId(), finalOutput));
            log.info("流程执行成功: flowId={}, runId={}, nodes={}", flow.getId(), run.getId(), nodeCount);
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

    private Map<String, Object> buildStepInput(NodeSpec spec, NodeContext ctx) {
        Map<String, Object> m = new HashMap<>();
        m.put("config", spec.config);
        m.put("variables", new HashMap<>(ctx.getVariables()));
        return m;
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
        if (run.getStartedAt() != null) {
            run.setCostMs(System.currentTimeMillis() - java.time.Duration.between(run.getStartedAt(), run.getFinishedAt()).toMillis());
        }
        runMapper.updateById(run);
    }

    /** 事件:流程执行成功 */
    public record FlowRunSuccessEvent(Long runId, Long flowId, Map<String, Object> output) {
    }

    /** 事件:流程执行失败 */
    public record FlowRunFailedEvent(Long runId, Long flowId, String errorMsg) {
    }
}
