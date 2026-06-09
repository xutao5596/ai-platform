package com.aiplatform.flow.chain;

import com.aiplatform.flow.spi.FlowNode;
import com.aiplatform.flow.registry.NodeRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ChainBuilder:把 LogicFlow 设计 JSON 转换为 LiteFlow Chain + 节点定义。
 * LogicFlow JSON 结构(简化):
 * {
 *   nodes: [
 *     {id: 'node_1', type: 'start', properties: {config: {...}}, x, y},
 *     {id: 'node_2', type: 'llm', properties: {config: {...}}}
 *   ],
 *   edges: [
 *     {sourceNodeId: 'node_1', targetNodeId: 'node_2'}
 *   ]
 * }
 *
 * 输出:ChainDescriptor(包含 node 列表 + 编排 + EL 表达式)
 *  - Serial: 序列化的执行链
 *  - Order:  节点 + 边
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChainBuilder {

    private final NodeRegistry nodeRegistry;
    private final ObjectMapper mapper = new ObjectMapper();

    public ChainDescriptor build(String designJson) {
        if (designJson == null || designJson.isBlank()) {
            return new ChainDescriptor(new ArrayList<>(), new ArrayList<>(), "");
        }
        try {
            JsonNode root = mapper.readTree(designJson);
            JsonNode nodesJson = root.path("nodes");
            JsonNode edgesJson = root.path("edges");

            List<FlowNode> nodes = new ArrayList<>();
            List<ChainEdge> edges = new ArrayList<>();
            for (JsonNode n : nodesJson) {
                String typeKey = n.path("type").asText();
                FlowNode fn = nodeRegistry.get(typeKey);
                if (fn == null) {
                    log.warn("design 中包含未注册节点类型: {}", typeKey);
                }
                nodes.add(fn);
            }
            for (JsonNode e : edgesJson) {
                edges.add(new ChainEdge(
                        firstNonEmpty(e, "sourceNodeId", "source"),
                        firstNonEmpty(e, "targetNodeId", "target")
                ));
            }
            // EL:简单串联
            String el = buildEl(nodes, edges);
            return new ChainDescriptor(nodes, edges, el);
        } catch (Exception e) {
            log.warn("解析 design JSON 失败: {}", e.getMessage());
            return new ChainDescriptor(new ArrayList<>(), new ArrayList<>(), "");
        }
    }

    private String buildEl(List<FlowNode> nodes, List<ChainEdge> edges) {
        if (nodes.isEmpty()) return "";
        // 简化 EL:按节点顺序链式串接
        // 1. 找到 start 节点
        FlowNode start = nodes.stream()
                .filter(n -> n != null && "start".equals(n.getTypeKey()))
                .findFirst().orElse(null);
        if (start == null) {
            return "node_" + nodes.get(0).getTypeKey();
        }
        // 2. BFS 按 start 顺延
        Map<String, List<String>> adj = new HashMap<>();
        for (ChainEdge e : edges) {
            adj.computeIfAbsent(e.source, k -> new ArrayList<>()).add(e.target);
        }
        // 3. 节点 id 与 typeKey 映射:设计器中 node id 一般是 'node_xxx',与节点 typeKey 配合
        // 这里用 typeKey 作为 LiteFlow 组件名
        StringBuilder sb = new StringBuilder();
        List<String> order = new ArrayList<>();
        java.util.LinkedList<String> queue = new java.util.LinkedList<>();
        queue.add("start");
        java.util.Set<String> visited = new java.util.HashSet<>();
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            if (visited.contains(cur)) continue;
            visited.add(cur);
            order.add(cur);
            for (String nxt : adj.getOrDefault(cur, List.of())) {
                queue.add(nxt);
            }
        }
        for (int i = 0; i < order.size(); i++) {
            if (i > 0) sb.append(" -> ");
            sb.append(order.get(i));
        }
        return sb.toString();
    }

    public record ChainDescriptor(List<FlowNode> nodes, List<ChainEdge> edges, String el) {
    }

    public record ChainEdge(String source, String target) {
    }

    /** 把 design 转成可在后续执行的 NodeSpec 列表(供 FlowExecutor 使用) */
    public List<NodeSpec> buildSpecs(String designJson) {
        if (designJson == null || designJson.isBlank()) return List.of();
        try {
            JsonNode root = mapper.readTree(designJson);
            JsonNode nodesJson = root.path("nodes");
            JsonNode edgesJson = root.path("edges");

            // 节点 id -> 节点配置
            Map<String, NodeSpec> specs = new LinkedHashMap<>();
            for (JsonNode n : nodesJson) {
                String id = n.path("id").asText();
                String typeKey = n.path("type").asText();
                // 兼容:LogicFlow properties.config 与简化 data
                JsonNode cfg = n.path("properties").path("config");
                if (cfg.isMissingNode() || cfg.isNull()) {
                    cfg = n.path("data");
                }
                Map<String, Object> config = mapper.convertValue(cfg, Map.class);
                specs.put(id, new NodeSpec(id, typeKey, config == null ? new HashMap<>() : config));
            }
            // 出边
            for (JsonNode e : edgesJson) {
                String src = firstNonEmpty(e, "sourceNodeId", "source");
                String tgt = firstNonEmpty(e, "targetNodeId", "target");
                NodeSpec s = specs.get(src);
                if (s != null) s.next.add(tgt);
            }
            return new ArrayList<>(specs.values());
        } catch (Exception e) {
            log.warn("buildSpecs 失败: {}", e.getMessage());
            return List.of();
        }
    }

    private static String firstNonEmpty(JsonNode obj, String... names) {
        for (String n : names) {
            JsonNode v = obj.path(n);
            if (!v.isMissingNode() && !v.isNull() && !v.asText().isEmpty()) return v.asText();
        }
        return "";
    }

    public static class NodeSpec {
        public final String id;
        public final String typeKey;
        public final Map<String, Object> config;
        public final List<String> next = new ArrayList<>();

        public NodeSpec(String id, String typeKey, Map<String, Object> config) {
            this.id = id;
            this.typeKey = typeKey;
            this.config = config;
        }
    }
}
