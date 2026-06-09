-- ============================================================
-- V7: AI 向量索引持久化 (Sprint 3.1 Agent B)
-- ai_vector_index:Hnswlib 序列化 BLOB + 元数据
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS ai_vector_index;
CREATE TABLE ai_vector_index (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    kb_id           BIGINT          NOT NULL COMMENT '知识库 ID',
    dimension       INT             NOT NULL COMMENT '向量维度',
    index_blob      LONGBLOB                 COMMENT 'Hnswlib 序列化索引 (Max ~64MB)',
    element_count   INT             NOT NULL DEFAULT 0 COMMENT '元素个数',
    m               INT             NOT NULL DEFAULT 16  COMMENT 'Hnswlib M 参数',
    ef_construction INT             NOT NULL DEFAULT 200 COMMENT 'Hnswlib efConstruction',
    ef_search       INT             NOT NULL DEFAULT 50  COMMENT 'Hnswlib efSearch',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_kb (kb_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 向量索引持久化(Hnswlib BLOB)';

SET FOREIGN_KEY_CHECKS = 1;
