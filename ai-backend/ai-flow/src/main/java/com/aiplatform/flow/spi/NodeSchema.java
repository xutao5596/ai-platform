package com.aiplatform.flow.spi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 节点定义 schema。用于前端编辑器生成属性表单。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeSchema implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 配置项(节点属性的输入 schema) */
    private List<Property> inputs;

    /** 输出变量 schema */
    private List<Property> outputs;

    public static NodeSchema of(List<Property> inputs, List<Property> outputs) {
        return new NodeSchema(inputs, outputs);
    }
}
