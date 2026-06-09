-- ============================================================
-- V4: AI 对话 + 助手配置表(4 张)
-- ai_chat_session / ai_chat_message / ai_assistant_config
-- ai_assistant_event_sub
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS ai_chat_session;
CREATE TABLE ai_chat_session (
    id            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    project_id    BIGINT                   COMMENT '项目ID(项目内会话)',
    user_id       BIGINT          NOT NULL COMMENT '用户ID',
    title         VARCHAR(200)             COMMENT '会话标题',
    model_id      BIGINT                   COMMENT '主模型',
    system_prompt LONGTEXT                 COMMENT '系统提示词',
    temperature   DECIMAL(3,2)   DEFAULT 0.7 COMMENT '温度',
    max_tokens    INT            DEFAULT 4096 COMMENT '最大输出',
    kb_ids        VARCHAR(500)             COMMENT '关联知识库列表(逗号)',
    tools_enabled TEXT                     COMMENT '启用的工具 JSON',
    message_count INT            DEFAULT 0 COMMENT '消息数',
    token_used    BIGINT         DEFAULT 0 COMMENT '累计 token',
    status        TINYINT        DEFAULT 1 COMMENT '状态 1进行 0归档',
    pin           TINYINT        DEFAULT 0 COMMENT '置顶',
    create_time   DATETIME                 COMMENT '创建时间',
    update_time   DATETIME                 COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user (user_id),
    KEY idx_project (project_id),
    KEY idx_update (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话会话';

DROP TABLE IF EXISTS ai_chat_message;
CREATE TABLE ai_chat_message (
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    session_id    BIGINT      NOT NULL COMMENT '会话ID',
    role          VARCHAR(20) NOT NULL COMMENT 'user/assistant/system/tool',
    content       LONGTEXT             COMMENT '消息内容',
    reasoning     LONGTEXT             COMMENT '思考过程(深度思考模型)',
    tool_calls    LONGTEXT             COMMENT '工具调用 JSON',
    tool_call_id  VARCHAR(64)          COMMENT '工具调用 ID',
    name          VARCHAR(100)         COMMENT '工具/函数名',
    input_tokens  INT                  COMMENT '输入 token',
    output_tokens INT                  COMMENT '输出 token',
    cost_ms       INT                  COMMENT '耗时',
    status        TINYINT     DEFAULT 1 COMMENT '状态 1正常 0失败',
    error_msg     VARCHAR(500)         COMMENT '错误信息',
    create_time   DATETIME             COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_session (session_id),
    KEY idx_create (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话消息';

DROP TABLE IF EXISTS ai_assistant_config;
CREATE TABLE ai_assistant_config (
    id            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    project_id    BIGINT          NOT NULL COMMENT '项目ID',
    flow_id       BIGINT                   COMMENT '关联的 flow(助手复用 Flow)',
    name          VARCHAR(100)    NOT NULL COMMENT '助手名',
    description   VARCHAR(500)             COMMENT '描述',
    avatar        VARCHAR(255)             COMMENT '头像',
    persona       LONGTEXT                 COMMENT '人设/系统提示词',
    model_id      BIGINT                   COMMENT '主模型',
    temperature   DECIMAL(3,2)   DEFAULT 0.7 COMMENT '温度',
    kb_ids        VARCHAR(500)             COMMENT '关联知识库(逗号)',
    tools_enabled TEXT                     COMMENT '启用的工具 JSON',
    welcome_msg   VARCHAR(500)             COMMENT '欢迎语',
    status        TINYINT        DEFAULT 1 COMMENT '状态',
    create_by     VARCHAR(50)              COMMENT '创建人',
    create_time   DATETIME                 COMMENT '创建时间',
    update_by     VARCHAR(50)              COMMENT '更新人',
    update_time   DATETIME                 COMMENT '更新时间',
    deleted       TINYINT        DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 助手配置';

DROP TABLE IF EXISTS ai_assistant_event_sub;
CREATE TABLE ai_assistant_event_sub (
    id            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    project_id    BIGINT          NOT NULL COMMENT '项目ID',
    assistant_id  BIGINT          NOT NULL COMMENT '助手ID',
    event_type    VARCHAR(50)     NOT NULL COMMENT '事件类型',
    filter        VARCHAR(500)             COMMENT '过滤条件 JSON',
    enabled       TINYINT        DEFAULT 1 COMMENT '启用',
    description   VARCHAR(255)             COMMENT '说明',
    create_time   DATETIME                 COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_assistant_event (assistant_id, event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='助手事件订阅';

-- Quartz 任务表(系统层,5.x 标准表)
-- 由 Spring Boot starter-quartz 自动建表,无需手动

SET FOREIGN_KEY_CHECKS = 1;
