package com.aiplatform.flow.spi;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 节点执行上下文。
 *  - input: 流程入口输入
 *  - variables: 节点之间共享的变量(k -> v)
 *  - nodeConfig: 当前节点配置(来自 design JSON 的 properties)
 *  - runId/flowId: 执行追踪
 *  - projectId: 用于权限/数据隔离
 *  - modelId/kbId/promptId: 节点运行需要的资源 ID
 */
@Data
public class NodeContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 执行 ID */
    private Long runId;
    /** 流程 ID */
    private Long flowId;
    /** 项目 ID */
    private Long projectId;
    /** 当前节点 ID(画布上的) */
    private String nodeId;
    /** 当前节点类型 */
    private String nodeType;
    /** 流程入口输入 */
    private Map<String, Object> input;
    /** 节点间共享变量 */
    private Map<String, Object> variables;
    /** 当前节点配置(节点在画布上的 properties) */
    private Map<String, Object> nodeConfig;
    /** 流程全局配置(可选) */
    private Map<String, Object> flowConfig;
    /** 用户 ID(执行触发人) */
    private Long userId;

    /** 全局节点 spec 映射(nodeId -> NodeSpec),由 FlowRunner 设置 */
    private java.util.concurrent.ConcurrentHashMap<String, com.aiplatform.flow.chain.ChainBuilder.NodeSpec> specMap;

    public NodeContext() {
        this.input = new HashMap<>();
        this.variables = new ConcurrentHashMap<>();
        this.nodeConfig = new HashMap<>();
        this.flowConfig = new HashMap<>();
    }

    public Object getVariable(String key) {
        return variables == null ? null : variables.get(key);
    }

    public void putVariable(String key, Object value) {
        if (variables == null) variables = new ConcurrentHashMap<>();
        variables.put(key, value);
    }

    public Object getInput(String key) {
        return input == null ? null : input.get(key);
    }

    public Object getConfig(String key) {
        return nodeConfig == null ? null : nodeConfig.get(key);
    }

    public void setSpecMap(java.util.concurrent.ConcurrentHashMap<String, com.aiplatform.flow.chain.ChainBuilder.NodeSpec> specMap) {
        this.specMap = specMap;
    }

    public com.aiplatform.flow.chain.ChainBuilder.NodeSpec getSpec(String nodeId) {
        return specMap == null ? null : specMap.get(nodeId);
    }

    public String getConfigString(String key) {
        Object v = getConfig(key);
        return v == null ? null : String.valueOf(v);
    }

    public Long getConfigLong(String key) {
        Object v = getConfig(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
