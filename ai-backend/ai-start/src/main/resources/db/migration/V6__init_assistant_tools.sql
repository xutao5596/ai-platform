-- ============================================================
-- V6: AI 助手工具日志 + 助手专属会话/消息
-- (Sprint 3 Agent B)
-- ai_assistant_tool_log / ai_assistant_session / ai_assistant_message
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS ai_assistant_tool_log;
CREATE TABLE ai_assistant_tool_log (
    id            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    assistant_id  BIGINT          NOT NULL COMMENT '助手ID',
    session_id    BIGINT                   COMMENT '会话ID(助手专属)',
    tool_name     VARCHAR(100)    NOT NULL COMMENT '工具名',
    args          LONGTEXT                 COMMENT '入参 JSON',
    result        LONGTEXT                 COMMENT '出参 JSON',
    status        TINYINT        DEFAULT 1  COMMENT '状态 1成功 0失败',
    cost_ms       INT            DEFAULT 0  COMMENT '耗时',
    error_msg     VARCHAR(2000)            COMMENT '错误信息',
    create_time   DATETIME                  COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_assistant (assistant_id),
    KEY idx_session (session_id),
    KEY idx_tool (tool_name),
    KEY idx_create (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 助手工具调用日志';

DROP TABLE IF EXISTS ai_assistant_session;
CREATE TABLE ai_assistant_session (
    id            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    assistant_id  BIGINT          NOT NULL COMMENT '助手ID',
    user_id       BIGINT          NOT NULL COMMENT '用户ID',
    project_id    BIGINT                   COMMENT '项目ID',
    title         VARCHAR(200)             COMMENT '会话标题',
    model_id      BIGINT                   COMMENT '主模型',
    system_prompt LONGTEXT                 COMMENT '系统提示词',
    message_count INT            DEFAULT 0 COMMENT '消息数',
    status        TINYINT        DEFAULT 1 COMMENT '状态 1进行 0归档',
    create_time   DATETIME                 COMMENT '创建时间',
    update_time   DATETIME                 COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_assistant (assistant_id),
    KEY idx_user (user_id),
    KEY idx_project (project_id),
    KEY idx_update (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 助手专属会话';

DROP TABLE IF EXISTS ai_assistant_message;
CREATE TABLE ai_assistant_message (
    id            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    session_id    BIGINT          NOT NULL COMMENT '会话ID',
    role          VARCHAR(20)     NOT NULL COMMENT 'user/assistant/system/tool',
    content       LONGTEXT                 COMMENT '消息内容',
    tool_calls    LONGTEXT                 COMMENT '工具调用 JSON',
    tool_call_id  VARCHAR(64)              COMMENT '工具调用 ID',
    name          VARCHAR(100)             COMMENT '工具/函数名',
    input_tokens  INT                      COMMENT '输入 token',
    output_tokens INT                      COMMENT '输出 token',
    cost_ms       INT                      COMMENT '耗时',
    status        TINYINT        DEFAULT 1 COMMENT '状态 1正常 0失败',
    error_msg     VARCHAR(2000)            COMMENT '错误信息',
    create_time   DATETIME                 COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_session (session_id),
    KEY idx_create (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 助手消息';

SET FOREIGN_KEY_CHECKS = 1;
