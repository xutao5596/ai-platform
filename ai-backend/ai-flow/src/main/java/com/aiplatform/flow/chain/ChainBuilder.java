package com.aiplatform.flow.chain;

import com.aiplatform.flow.spi.FlowNode;
import com.aiplatform.flow.registry.NodeRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

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
 * Sprint 3.1 升级:
 *  - 节点 ID 用纯单词(typeKey,如 llm / end / if_else),绝不加点号
 *    (QLExpress 解析器会报 at line -1 错)
 *  - 生成 LiteFlow EL 表达式,供 FlowRunner 用 LiteFlowChainELBuilder 注册
 *  - 保留 build() + ChainDescriptor 以兼容历史 E2E 契约
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
            String el = buildEl(designJson);
            return new ChainDescriptor(nodes, edges, el);
        } catch (Exception e) {
            log.warn("解析 design JSON 失败: {}", e.getMessage());
            return new ChainDescriptor(new ArrayList<>(), new ArrayList<>(), "");
        }
    }

    /**
     * 从 design JSON 直接生成 LiteFlow EL 字符串(纯单词节点 ID,无点号)。
     * 入口方法,供 FlowRunner 在运行时调用。
     */
    public String buildEl(String designJson) {
        if (designJson == null || designJson.isBlank()) return "";
        try {
            JsonNode root = mapper.readTree(designJson);
            JsonNode nodesJson = root.path("nodes");
            JsonNode edgesJson = root.path("edges");
            Map<String, String> idToTypeKey = new LinkedHashMap<>();
            for (JsonNode n : nodesJson) {
                String id = n.path("id").asText();
                String typeKey = n.path("type").asText();
                idToTypeKey.put(id, sanitizeId(typeKey));
            }
            Map<String, List<String>> adj = new HashMap<>();
            for (JsonNode e : edgesJson) {
                String src = firstNonEmpty(e, "sourceNodeId", "source");
                String tgt = firstNonEmpty(e, "targetNodeId", "target");
                if (src == null || tgt == null || src.isEmpty() || tgt.isEmpty()) continue;
                adj.computeIfAbsent(src, k -> new ArrayList<>()).add(tgt);
            }
            String start = findStartId(idToTypeKey);
            if (start == null) {
                if (idToTypeKey.isEmpty()) return "";
                start = idToTypeKey.keySet().iterator().next();
            }
            List<String> order = bfs(start, adj);
            List<String> typeOrder = new ArrayList<>();
            for (String id : order) {
                String t = idToTypeKey.get(id);
                if (t != null && !t.isEmpty()) typeOrder.add(t);
            }
            return joinThen(typeOrder);
        } catch (Exception e) {
            log.warn("buildEl(designJson) 失败: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 从 NodeSpec 列表(已经 buildSpecs 过的)生成 LiteFlow EL。
     * 同样使用 typeKey 作为节点 ID(纯单词)。
     */
    public String buildEl(List<NodeSpec> specs) {
        if (specs == null || specs.isEmpty()) return "";
        Map<String, NodeSpec> byId = new LinkedHashMap<>();
        for (NodeSpec s : specs) {
            byId.put(s.id, s);
        }
        Map<String, List<String>> adj = new HashMap<>();
        for (NodeSpec s : specs) {
            for (String nxt : s.next) {
                adj.computeIfAbsent(s.id, k -> new ArrayList<>()).add(nxt);
            }
        }
        NodeSpec start = specs.stream()
                .filter(s -> "start".equals(s.typeKey))
                .findFirst().orElse(null);
        String startId = start != null ? start.id : specs.get(0).id;
        List<String> order = bfs(startId, adj);
        List<String> typeOrder = new ArrayList<>();
        for (String id : order) {
            NodeSpec ns = byId.get(id);
            if (ns == null) continue;
            String t = sanitizeId(ns.typeKey);
            if (!t.isEmpty()) typeOrder.add(t);
        }
        return joinThen(typeOrder);
    }

    private List<String> bfs(String start, Map<String, List<String>> adj) {
        List<String> order = new ArrayList<>();
        if (start == null) return order;
        Set<String> visited = new HashSet<>();
        LinkedList<String> q = new LinkedList<>();
        q.add(start);
        while (!q.isEmpty()) {
            String cur = q.poll();
            if (visited.contains(cur)) continue;
            visited.add(cur);
            order.add(cur);
            for (String nxt : adj.getOrDefault(cur, List.of())) {
                if (!visited.contains(nxt)) q.add(nxt);
            }
        }
        return order;
    }

    private String joinThen(List<String> ids) {
        if (ids.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("THEN(");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(ids.get(i));
        }
        sb.append(")");
        return sb.toString();
    }

    private String findStartId(Map<String, String> idToTypeKey) {
        for (Map.Entry<String, String> e : idToTypeKey.entrySet()) {
            if ("start".equals(e.getValue())) return e.getKey();
        }
        return null;
    }

    private String sanitizeId(String s) {
        if (s == null) return "";
        return s.trim();
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

            Map<String, NodeSpec> specs = new LinkedHashMap<>();
            for (JsonNode n : nodesJson) {
                String id = n.path("id").asText();
                String typeKey = n.path("type").asText();
                JsonNode cfg = n.path("properties").path("config");
                if (cfg.isMissingNode() || cfg.isNull()) {
                    cfg = n.path("data");
                }
                Map<String, Object> config = mapper.convertValue(cfg, Map.class);
                specs.put(id, new NodeSpec(id, typeKey, config == null ? new HashMap<>() : config));
            }
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
