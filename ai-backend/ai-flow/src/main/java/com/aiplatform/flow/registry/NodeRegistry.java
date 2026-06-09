package com.aiplatform.flow.registry;

import com.aiplatform.flow.spi.FlowNode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 节点注册中心:启动时自动扫描所有 FlowNode Bean,按 typeKey 索引。
 * 同时支持运行时热注册(自定义节点)。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NodeRegistry {

    private final ApplicationContext applicationContext;

    /** typeKey -> FlowNode */
    private final Map<String, FlowNode> nodes = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        Map<String, FlowNode> beans = applicationContext.getBeansOfType(FlowNode.class);
        for (FlowNode node : beans.values()) {
            register(node);
        }
        log.info("NodeRegistry 初始化完成,共注册 {} 个内置节点: {}",
                nodes.size(),
                nodes.keySet());
    }

    /** 注册节点(自定义节点热加载用) */
    public synchronized void register(FlowNode node) {
        String key = node.getTypeKey();
        if (key == null || key.isBlank()) {
            log.warn("节点 {} 缺少 typeKey,已忽略", node.getClass().getName());
            return;
        }
        nodes.put(key, node);
        log.debug("注册节点: typeKey={}, name={}", key, node.getDisplayName());
    }

    public synchronized void unregister(String typeKey) {
        nodes.remove(typeKey);
    }

    public FlowNode get(String typeKey) {
        return nodes.get(typeKey);
    }

    public boolean contains(String typeKey) {
        return nodes.containsKey(typeKey);
    }

    public Collection<FlowNode> all() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public List<FlowNode> list() {
        return List.copyOf(nodes.values());
    }

    public List<FlowNode> listByCategory(String category) {
        return nodes.values().stream()
                .filter(n -> category == null || category.equals(n.getCategory()))
                .collect(Collectors.toList());
    }
}
