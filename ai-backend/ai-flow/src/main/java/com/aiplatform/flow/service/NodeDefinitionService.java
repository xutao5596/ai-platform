package com.aiplatform.flow.service;

import com.aiplatform.flow.dto.NodeDefinitionVO;
import com.aiplatform.flow.registry.NodeRegistry;
import com.aiplatform.flow.spi.FlowNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 节点定义查询服务。
 */
@Service
@RequiredArgsConstructor
public class NodeDefinitionService {

    private final NodeRegistry nodeRegistry;

    public List<NodeDefinitionVO> listAll() {
        return nodeRegistry.all().stream()
                .sorted(Comparator.comparingInt(n -> order(n.getCategory())))
                .map(NodeDefinitionVO::of)
                .collect(Collectors.toList());
    }

    public Map<String, List<NodeDefinitionVO>> listByCategory() {
        List<NodeDefinitionVO> all = listAll();
        return all.stream().collect(Collectors.groupingBy(NodeDefinitionVO::getCategory));
    }

    public List<String> categories() {
        return List.of("basic", "ai", "control", "tool", "data");
    }

    private int order(String c) {
        if (c == null) return 99;
        return switch (c) {
            case "basic" -> 1;
            case "ai" -> 2;
            case "control" -> 3;
            case "tool" -> 4;
            case "data" -> 5;
            default -> 99;
        };
    }
}
