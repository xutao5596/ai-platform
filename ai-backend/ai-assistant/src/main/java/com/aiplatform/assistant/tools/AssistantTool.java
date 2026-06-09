package com.aiplatform.assistant.tools;

import java.util.Map;

/**
 * 助手工具 SPI:每个工具实现此接口,由 ToolRegistry 自动扫描(@Component)。
 */
public interface AssistantTool {

    /**
     * 工具名(LLM 调用的标识,JSON 中作为 "tool" 字段的值)。
     * 例:"http_request" / "search_kb" / "calculator"
     */
    String name();

    /**
     * 工具显示名(UI 展示用)。
     */
    String displayName();

    /**
     * 工具描述(供 LLM 理解:何时调用,做什么)。
     */
    String description();

    /**
     * 工具分类:基础 / AI / 控制 / 工具 / 数据。
     */
    String category();

    /**
     * 参数 schema(JSON 字符串,描述参数 key/type/required)。
     * 例:"{\"url\":\"string,required\",\"method\":\"string,default:GET\"}"
     */
    String parametersSchema();

    /**
     * 实际执行逻辑。
     *
     * @param args    工具参数
     * @param context 工具上下文(assistantId/sessionId/userId)
     * @return 执行结果
     */
    ToolResult execute(Map<String, Object> args, ToolContext context);
}
