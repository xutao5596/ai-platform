package com.aiplatform.flow.spi;

/**
 * 流程节点 SPI。所有内置节点和开发者扩展节点均实现此接口。
 * 通过 @Component 注册到 NodeRegistry。
 */
public interface FlowNode {

    /** 节点类型 Key(全局唯一,小写蛇形命名):start / end / llm / ... */
    String getTypeKey();

    /** 节点展示名(中文) */
    String getDisplayName();

    /** 节点分类:basic / ai / control / tool / data */
    String getCategory();

    /** 节点图标(Iconify 名称或 emoji) */
    String getIcon();

    /** 节点卡片颜色(HEX) */
    String getColor();

    /** 节点配置 schema(用于前端表单生成) */
    NodeSchema getSchema();

    /** 节点描述 */
    default String getDescription() {
        return getDisplayName();
    }

    /** 执行节点。ctx 中包含 input/variables/nodeConfig,返回 output 写入 variables。 */
    NodeExecuteResult execute(NodeContext ctx) throws Exception;
}
