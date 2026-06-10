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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 结束节点:把指定 variables 写入最终 output。
 */
@Slf4j
@LiteflowComponent("end")
@Component
public class EndNode extends NodeComponent implements FlowNode {

    @Override
    public String getTypeKey() {
        return "end";
    }

    @Override
    public String getDisplayName() {
        return "结束";
    }

    @Override
    public String getCategory() {
        return "basic";
    }

    @Override
    public String getIcon() {
        return "mdi:stop-circle";
    }

    @Override
    public String getColor() {
        return "#909399";
    }

    @Override
    public String getDescription() {
        return "流程出口节点,把当前 variables 写入流程输出。";
    }

    @Override
    public NodeSchema getSchema() {
        return NodeSchema.of(
                List.of(
                        new Property("outputKey", "输出变量 Key", "string", false,
                                "result", "把 variables 中的哪个 key 作为最终输出(空则全部)", null)
                ),
                List.of(
                        new Property("result", "结果", "json", false, null, "最终输出内容", null)
                )
        );
    }

    @Override
    public NodeExecuteResult execute(NodeContext ctx) {
        String key = ctx.getConfigString("outputKey");
        Map<String, Object> out = new HashMap<>();
        if (key != null && !key.isBlank()) {
            Object v = ctx.getVariable(key);
            out.put(key, v);
            out.put("result", v);
        } else {
            if (ctx.getVariables() != null) {
                out.putAll(ctx.getVariables());
            }
        }
        return NodeExecuteResult.success(out);
    }

    @Override
    public void process() throws Exception {
        NodeContext ctx = this.getContextBean(NodeContext.class);
        if (ctx == null) {
            log.warn("EndNode 收到空 NodeContext,跳过");
            return;
        }
        execute(ctx);
    }
}
