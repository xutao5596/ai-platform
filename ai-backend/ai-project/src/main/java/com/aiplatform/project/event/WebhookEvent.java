package com.aiplatform.project.event;

/**
 * Webhook 事件类型常量(存到 ai_project_webhook.events 字段,JSON 数组)。
 * 命名规则: 领域.动作.结果,使用点分 kebab,与系统事件总线一致。
 */
public final class WebhookEvent {

    private WebhookEvent() {}

    /** 流程执行成功 */
    public static final String FLOW_RUN_SUCCESS = "flow.run.success";
    /** 流程执行失败 */
    public static final String FLOW_RUN_FAILED = "flow.run.failed";
    /** 助手对话完成 */
    public static final String ASSISTANT_CHAT_COMPLETED = "assistant.chat.completed";
    /** 知识库文档索引完成 */
    public static final String KB_DOC_INDEXED = "kb.doc.indexed";
    /** 测试事件(手动触发 /test 端点) */
    public static final String WEBHOOK_TEST = "webhook.test";
}
