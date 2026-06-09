package com.aiplatform.flow.dto;

import com.aiplatform.flow.spi.FlowNode;
import com.aiplatform.flow.spi.NodeSchema;
import com.aiplatform.flow.spi.Property;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 节点定义 DTO(用于前端展示)。
 */
@Data
public class NodeDefinitionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String typeKey;
    private String displayName;
    private String description;
    private String category;
    private String icon;
    private String color;
    private List<Property> inputs;
    private List<Property> outputs;

    public static NodeDefinitionVO of(FlowNode node) {
        NodeDefinitionVO vo = new NodeDefinitionVO();
        vo.typeKey = node.getTypeKey();
        vo.displayName = node.getDisplayName();
        vo.description = node.getDescription();
        vo.category = node.getCategory();
        vo.icon = node.getIcon();
        vo.color = node.getColor();
        NodeSchema s = node.getSchema();
        if (s != null) {
            vo.inputs = s.getInputs() == null ? new ArrayList<>() : s.getInputs();
            vo.outputs = s.getOutputs() == null ? new ArrayList<>() : s.getOutputs();
        } else {
            vo.inputs = new ArrayList<>();
            vo.outputs = new ArrayList<>();
        }
        return vo;
    }
}
