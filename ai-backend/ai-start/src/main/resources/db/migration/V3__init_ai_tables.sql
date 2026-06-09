-- ============================================================
-- V3: AI 资源(全局) + AI 资源(项目隔离)
-- 共 7 张:ai_model, ai_mcp, ai_knowledge, ai_knowledge_doc,
--         ai_knowledge_chunk, ai_prompt, ai_prompt_version
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 全局共享
DROP TABLE IF EXISTS ai_model;
CREATE TABLE ai_model (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    name            VARCHAR(100)    NOT NULL COMMENT '模型名称(展示)',
    provider        VARCHAR(50)     NOT NULL COMMENT '厂商: openai/deepseek/claude/qwen/glm/ollama',
    model_name      VARCHAR(100)    NOT NULL COMMENT '模型标识(如 gpt-4 / deepseek-chat)',
    api_base        VARCHAR(255)             COMMENT 'API Base URL',
    api_key         VARCHAR(255)    NOT NULL COMMENT 'API Key',
    proxy_enabled   TINYINT        DEFAULT 0 COMMENT '是否启用代理',
    proxy_url       VARCHAR(255)             COMMENT '代理 URL',
    max_tokens      INT            DEFAULT 4096 COMMENT '最大输出 token',
    temperature     DECIMAL(3,2)   DEFAULT 0.70 COMMENT '默认温度 0-2',
    top_p           DECIMAL(3,2)   DEFAULT 1.00 COMMENT '默认 top_p',
    embedding_model VARCHAR(100)             COMMENT '对应的 embedding 模型名(用于知识库)',
    dimension       INT                      COMMENT '向量维度(embedding 用)',
    status          TINYINT        DEFAULT 1 COMMENT '状态 1启用 0停用',
    is_default      TINYINT        DEFAULT 0 COMMENT '默认模型 1是 0否',
    description     VARCHAR(500)             COMMENT '描述',
    create_by       VARCHAR(50)              COMMENT '创建人',
    create_time     DATETIME                 COMMENT '创建时间',
    update_by       VARCHAR(50)              COMMENT '更新人',
    update_time     DATETIME                 COMMENT '更新时间',
    deleted         TINYINT        DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_provider_model (provider, model_name, deleted),
    KEY idx_provider (provider)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 模型';

DROP TABLE IF EXISTS ai_mcp;
CREATE TABLE ai_mcp (
    id            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    name          VARCHAR(100)    NOT NULL COMMENT '名称',
    description   VARCHAR(500)             COMMENT '描述',
    transport     VARCHAR(20)     NOT NULL DEFAULT 'stdio' COMMENT '传输方式 stdio/sse/http',
    command       VARCHAR(500)             COMMENT 'stdio 启动命令',
    args          TEXT                     COMMENT 'stdio 参数(JSON 数组)',
    env           TEXT                     COMMENT '环境变量(JSON 对象)',
    url           VARCHAR(500)             COMMENT 'sse/http URL',
    headers       TEXT                     COMMENT 'sse/http headers',
    tools         TEXT                     COMMENT '已发现工具列表(JSON)',
    status        TINYINT         DEFAULT 1 COMMENT '状态',
    last_test_at  DATETIME                 COMMENT '最后测试时间',
    last_test_ok  TINYINT                  COMMENT '最后测试结果 1成功 0失败',
    create_by     VARCHAR(50)              COMMENT '创建人',
    create_time   DATETIME                 COMMENT '创建时间',
    update_by     VARCHAR(50)              COMMENT '更新人',
    update_time   DATETIME                 COMMENT '更新时间',
    deleted       TINYINT         DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP 服务';

-- 项目隔离
DROP TABLE IF EXISTS ai_knowledge;
CREATE TABLE ai_knowledge (
    id            BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'id',
    project_id    BIGINT          NOT NULL COMMENT 'project id',
    name          VARCHAR(100)    NOT NULL COMMENT 'name',
    description   VARCHAR(500)             COMMENT 'description',
    model_id      BIGINT                   COMMENT 'embedding model id',
    chunk_size    INT            DEFAULT 500 COMMENT 'chunk size',
    chunk_overlap INT            DEFAULT 50  COMMENT 'chunk overlap',
    sep           VARCHAR(50)              COMMENT 'segment separator',
    doc_count     INT            DEFAULT 0   COMMENT 'document count',
    chunk_count   INT            DEFAULT 0   COMMENT 'chunk count',
    status        TINYINT        DEFAULT 1   COMMENT 'status',
    create_by     VARCHAR(50)                COMMENT '创建人',
    create_time   DATETIME                   COMMENT '创建时间',
    update_by     VARCHAR(50)                COMMENT '更新人',
    update_time   DATETIME                   COMMENT '更新时间',
    deleted       TINYINT        DEFAULT 0   COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库';

DROP TABLE IF EXISTS ai_knowledge_doc;
CREATE TABLE ai_knowledge_doc (
    id            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    kb_id         BIGINT          NOT NULL COMMENT '知识库ID',
    project_id    BIGINT          NOT NULL COMMENT '项目ID(冗余)',
    name          VARCHAR(255)    NOT NULL COMMENT '文件名',
    original_name VARCHAR(255)    NOT NULL COMMENT '原始文件名',
    file_path     VARCHAR(500)    NOT NULL COMMENT '文件路径',
    file_size     BIGINT                   COMMENT '文件大小',
    content_type  VARCHAR(100)             COMMENT 'MIME',
    status        VARCHAR(20)     DEFAULT 'pending' COMMENT 'pending/parsing/ready/failed',
    progress      TINYINT         DEFAULT 0 COMMENT '解析进度 0-100',
    chunk_count   INT            DEFAULT 0 COMMENT '分段数',
    error_msg     TEXT                     COMMENT '解析错误信息',
    parse_time    DATETIME                 COMMENT '解析完成时间',
    create_by     VARCHAR(50)              COMMENT '上传人',
    create_time   DATETIME                 COMMENT '创建时间',
    update_time   DATETIME                 COMMENT '更新时间',
    deleted       TINYINT        DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_kb (kb_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档';

DROP TABLE IF EXISTS ai_knowledge_chunk;
CREATE TABLE ai_knowledge_chunk (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    doc_id       BIGINT      NOT NULL COMMENT '文档ID',
    kb_id        BIGINT      NOT NULL COMMENT '知识库ID',
    project_id   BIGINT      NOT NULL COMMENT '项目ID',
    chunk_index  INT                  COMMENT '段序号',
    content      LONGTEXT             COMMENT '段内容',
    content_len  INT                  COMMENT '段长度',
    vector_id    BIGINT      NOT NULL COMMENT 'Hnswlib 内部 ID',
    score        FLOAT                 COMMENT '检索得分(查询时填)',
    metadata     LONGTEXT             COMMENT '元数据 JSON',
    create_time  DATETIME             COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_doc (doc_id),
    KEY idx_kb (kb_id),
    KEY idx_vector (vector_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库分段';

DROP TABLE IF EXISTS ai_prompt;
CREATE TABLE ai_prompt (
    id            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    project_id    BIGINT          NOT NULL COMMENT '项目ID',
    name          VARCHAR(100)    NOT NULL COMMENT '提示词名',
    code          VARCHAR(50)     NOT NULL COMMENT '编码(项目内唯一)',
    description   VARCHAR(500)             COMMENT '描述',
    current_version_id BIGINT              COMMENT '当前激活版本ID',
    version_count INT            DEFAULT 0  COMMENT '版本数',
    status        TINYINT        DEFAULT 1  COMMENT '状态',
    create_by     VARCHAR(50)               COMMENT '创建人',
    create_time   DATETIME                  COMMENT '创建时间',
    update_by     VARCHAR(50)               COMMENT '更新人',
    update_time   DATETIME                  COMMENT '更新时间',
    deleted       TINYINT        DEFAULT 0  COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_code (project_id, code, deleted),
    KEY idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提示词';

DROP TABLE IF EXISTS ai_prompt_version;
CREATE TABLE ai_prompt_version (
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    prompt_id     BIGINT      NOT NULL COMMENT '提示词ID',
    project_id    BIGINT      NOT NULL COMMENT '项目ID',
    version       INT         NOT NULL COMMENT '版本号',
    content       LONGTEXT    NOT NULL COMMENT '模板内容',
    variables     LONGTEXT             COMMENT '变量定义 JSON',
    model_id      BIGINT               COMMENT '关联的模型',
    temperature   DECIMAL(3,2)         COMMENT '温度',
    max_tokens    INT                  COMMENT '最大输出',
    changelog     VARCHAR(500)         COMMENT '变更说明',
    is_active     TINYINT    DEFAULT 0 COMMENT '是否当前激活版本',
    create_by     VARCHAR(50)          COMMENT '创建人',
    create_time   DATETIME             COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_prompt_version (prompt_id, version),
    KEY idx_prompt (prompt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提示词版本';

SET FOREIGN_KEY_CHECKS = 1;
