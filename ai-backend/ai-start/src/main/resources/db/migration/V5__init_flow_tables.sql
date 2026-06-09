-- ============================================================
-- V5: 流程引擎表(6 张)
-- ai_flow / ai_flow_version / ai_flow_trigger / ai_flow_run
-- ai_flow_run_step / ai_custom_node
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS ai_flow;
CREATE TABLE ai_flow (
    id                 BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    project_id         BIGINT          NOT NULL COMMENT '项目ID',
    name               VARCHAR(100)    NOT NULL COMMENT '流程名',
    description        VARCHAR(500)             COMMENT '描述',
    icon               VARCHAR(255)             COMMENT '图标',
    is_assistant       TINYINT        DEFAULT 0 COMMENT '是否助手 1是 0否',
    design             LONGTEXT                 COMMENT 'LogicFlow 设计 JSON',
    chain              LONGTEXT                 COMMENT 'LiteFlow Chain(缓存)',
    status             VARCHAR(20)     DEFAULT 'draft' COMMENT 'draft/published/archived',
    current_version_id BIGINT                   COMMENT '当前发布版本ID',
    create_by          VARCHAR(50)              COMMENT '创建人',
    create_time        DATETIME                 COMMENT '创建时间',
    update_by          VARCHAR(50)              COMMENT '更新人',
    update_time        DATETIME                 COMMENT '更新时间',
    deleted            TINYINT        DEFAULT 0  COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_project (project_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程';

DROP TABLE IF EXISTS ai_flow_version;
CREATE TABLE ai_flow_version (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    flow_id    BIGINT       NOT NULL COMMENT '流程ID',
    project_id BIGINT       NOT NULL COMMENT '项目ID',
    version    INT          NOT NULL COMMENT '版本号',
    design     LONGTEXT              COMMENT 'LogicFlow 设计 JSON',
    chain      LONGTEXT              COMMENT 'LiteFlow Chain',
    changelog  VARCHAR(500)          COMMENT '变更说明',
    is_active  TINYINT     DEFAULT 0 COMMENT '是否当前发布版本',
    create_by  VARCHAR(50)           COMMENT '创建人',
    create_time DATETIME              COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_flow_version (flow_id, version),
    KEY idx_flow (flow_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程版本';

DROP TABLE IF EXISTS ai_flow_trigger;
CREATE TABLE ai_flow_trigger (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    flow_id         BIGINT       NOT NULL COMMENT '流程ID',
    project_id      BIGINT       NOT NULL COMMENT '项目ID',
    type            VARCHAR(20)  NOT NULL COMMENT 'manual/cron/webhook/event/chained',
    config          TEXT                  COMMENT '触发器配置 JSON',
    status          TINYINT     DEFAULT 1 COMMENT '1启用 0禁用',
    last_run_at     DATETIME              COMMENT '最后执行时间',
    last_run_status VARCHAR(20)           COMMENT 'success/failed',
    create_by       VARCHAR(50)           COMMENT '创建人',
    create_time     DATETIME              COMMENT '创建时间',
    update_by       VARCHAR(50)           COMMENT '更新人',
    update_time     DATETIME              COMMENT '更新时间',
    deleted         TINYINT     DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_flow (flow_id),
    KEY idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程触发器';

DROP TABLE IF EXISTS ai_flow_run;
CREATE TABLE ai_flow_run (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    flow_id     BIGINT       NOT NULL COMMENT '流程ID',
    project_id  BIGINT       NOT NULL COMMENT '项目ID',
    version_id  BIGINT                COMMENT '执行的版本ID',
    trigger_type VARCHAR(20) NOT NULL COMMENT 'manual/cron/webhook/event/chained',
    status      VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/running/success/failed',
    input       LONGTEXT              COMMENT '输入 JSON',
    output      LONGTEXT              COMMENT '输出 JSON',
    error_msg   TEXT                  COMMENT '错误信息',
    cost_ms     BIGINT                COMMENT '耗时 ms',
    started_at  DATETIME              COMMENT '开始时间',
    finished_at DATETIME              COMMENT '完成时间',
    create_by   VARCHAR(50)           COMMENT '创建人',
    create_time DATETIME              COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_flow (flow_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程执行记录';

DROP TABLE IF EXISTS ai_flow_run_step;
CREATE TABLE ai_flow_run_step (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    run_id      BIGINT       NOT NULL COMMENT '执行ID',
    node_id     VARCHAR(64)  NOT NULL COMMENT '节点ID(画布上的)',
    node_type   VARCHAR(50)  NOT NULL COMMENT '节点类型(start/llm/...)',
    status      VARCHAR(20)           COMMENT 'pending/running/success/failed',
    input       LONGTEXT              COMMENT '节点输入 JSON',
    output      LONGTEXT              COMMENT '节点输出 JSON',
    cost_ms     BIGINT                COMMENT '耗时 ms',
    started_at  DATETIME              COMMENT '开始',
    finished_at DATETIME              COMMENT '结束',
    create_time DATETIME              COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_run (run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程执行步骤';

DROP TABLE IF EXISTS ai_custom_node;
CREATE TABLE ai_custom_node (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    project_id      BIGINT       NOT NULL COMMENT '项目ID',
    name            VARCHAR(100) NOT NULL COMMENT '节点名',
    type_key        VARCHAR(50)  NOT NULL COMMENT '类型 Key(项目内唯一)',
    category        VARCHAR(20)           COMMENT '分类',
    config_schema   TEXT                  COMMENT '配置 schema JSON',
    implementation  TEXT                  COMMENT '实现(脚本/Java 类名)',
    status          TINYINT     DEFAULT 1 COMMENT '状态',
    create_by       VARCHAR(50)           COMMENT '创建人',
    create_time     DATETIME              COMMENT '创建时间',
    update_by       VARCHAR(50)           COMMENT '更新人',
    update_time     DATETIME              COMMENT '更新时间',
    deleted         TINYINT     DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_key (project_id, type_key, deleted),
    KEY idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义节点';

SET FOREIGN_KEY_CHECKS = 1;
