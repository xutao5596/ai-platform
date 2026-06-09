package com.aiplatform.assistant.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 子流程调用工具:PoC 实现 — 通过反射调用 ai-flow 模块的 FlowService(如果存在)。
 * Sprint 3 Agent A 的 FlowService 签名预计为 runFlow(flowId, input) → result。
 * 如果 FlowService 不可用,返回明确错误(不影响启动)。
 */
@Slf4j
@Component
public class SubflowTool implements AssistantTool {

    private final ApplicationContext applicationContext;

    public SubflowTool(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public String name() {
        return "run_flow";
    }

    @Override
    public String displayName() {
        return "运行子流程";
    }

    @Override
    public String description() {
        return "调用项目内的某个 Flow(流程)并返回其执行结果。需要提供 flowId 和入参 input(对象)。";
    }

    @Override
    public String category() {
        return "控制";
    }

    @Override
    public String parametersSchema() {
        return "{\"flowId\":\"number,required\",\"input\":\"object,optional\"}";
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext context) {
        Object flowIdObj = args.get("flowId");
        if (flowIdObj == null) return ToolResult.fail("flowId 不能为空");
        long flowId = ((Number) flowIdObj).longValue();
        Object input = args.get("input");
        try {
            // 尝试从 Spring 容器中拿 FlowService(Sprint 3 Agent A 实现)
            String[] candidateNames = {"flowService", "FlowService"};
            Object flowService = null;
            for (String n : candidateNames) {
                try {
                    flowService = applicationContext.getBean(n);
                    if (flowService != null) break;
                } catch (Exception ignore) {
                }
            }
            if (flowService == null) {
                // 扫描所有 bean,找带 run/runFlow/runFlowSync 方法的
                Map<String, Object> beans = applicationContext.getBeansWithAnnotation(org.springframework.stereotype.Service.class);
                for (Object bean : beans.values()) {
                    if (bean.getClass().getName().endsWith(".FlowService")
                            || bean.getClass().getSimpleName().equals("FlowService")) {
                        flowService = bean;
                        break;
                    }
                }
            }
            if (flowService == null) {
                return ToolResult.fail("FlowService 不可用(可能 Sprint 3 Agent A 尚未合并 FlowService Bean)");
            }
            // 尝试反射调用 runFlow / run 方法
            Method m = null;
            for (String mName : new String[]{"runFlow", "run", "execute"}) {
                try {
                    Method candidate = flowService.getClass().getMethod(mName, long.class, Object.class);
                    m = candidate;
                    break;
                } catch (NoSuchMethodException ex) {
                    try {
                        Method candidate2 = flowService.getClass().getMethod(mName, Long.class, Object.class);
                        m = candidate2;
                        break;
                    } catch (NoSuchMethodException ignore) {
                    }
                }
            }
            if (m == null) {
                return ToolResult.fail("FlowService 未找到 runFlow(long,Object) 方法,当前签名不匹配");
            }
            Object result = m.invoke(flowService, flowId, input);
            return ToolResult.ok("Flow " + flowId + " executed", String.valueOf(result));
        } catch (Exception e) {
            log.warn("SubflowTool error: flowId={}", flowId, e);
            return ToolResult.fail("子流程执行失败: " + e.getMessage());
        }
    }
}
