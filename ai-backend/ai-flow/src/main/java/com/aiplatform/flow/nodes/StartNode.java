package com.aiplatform.flow.nodes;

import com.aiplatform.flow.spi.FlowNode;
import com.aiplatform.flow.spi.NodeContext;
import com.aiplatform.flow.spi.NodeExecuteResult;
import com.aiplatform.flow.spi.NodeSchema;
import com.aiplatform.flow.spi.Property;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 起始节点:把入参(input)写入 variables。
 * 任何流程都必须以 start 节点开始。
 */
@Slf4j
@LiteflowComponent("start")
@Component
public class StartNode extends NodeComponent implements FlowNode {

    @Override
    public String getTypeKey() {
        return "start";
    }

    @Override
    public String getDisplayName() {
        return "开始";
    }

    @Override
    public String getCategory() {
        return "basic";
    }

    @Override
    public String getIcon() {
        return "mdi:play-circle";
    }

    @Override
    public String getColor() {
        return "#67C23A";
    }

    @Override
    public String getDescription() {
        return "流程入口节点,把入参写入共享变量。";
    }

    @Override
    public NodeSchema getSchema() {
        return NodeSchema.of(
                List.of(
                        new Property("inputMappings", "入参映射", "json", false,
                                "{}", "把 ctx.input 的字段映射到 variables(可选)", null)
                ),
                new ArrayList<>()
        );
    }

    @Override
    public NodeExecuteResult execute(NodeContext ctx) {
        if (ctx.getInput() != null) {
            for (Map.Entry<String, Object> e : ctx.getInput().entrySet()) {
                ctx.putVariable(e.getKey(), e.getValue());
            }
        }
        Map<String, Object> out = new HashMap<>();
        out.put("started", true);
        return NodeExecuteResult.success(out);
    }

    @Override
    public void process() throws Exception {
        NodeContext ctx = this.getContextBean(NodeContext.class);
        if (ctx != null) {
            var spec = ctx.getSpec(this.getNodeId());
            if (spec != null) ctx.setNodeConfig(spec.config);
        }
        if (ctx == null) {
            log.warn("StartNode 收到空 NodeContext,跳过");
            return;
        }
        execute(ctx);
    }
}
