package com.aiplatform.ai.vector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * ai_vector_index 表 JDBC 访问层。
 * Hnswlib 序列化 BLOB 读写。
 *
 * 不强制要求 Spring 注入 JdbcTemplate:ai-ai 模块未直接依赖 spring-jdbc starter,
 * 改用 ApplicationContext 懒查(和 PreProjectRoleAspect 同样模式)。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorIndexPersistence {

    private final ApplicationContext applicationContext;
    private JdbcTemplate jdbc;

    @PostConstruct
    void init() {
        try {
            this.jdbc = applicationContext.getBean(JdbcTemplate.class);
        } catch (Exception e) {
            log.warn("[VectorIndexPersistence] JdbcTemplate 未找到,持久化将不可用: {}", e.getMessage());
        }
    }

    /**
     * 读取索引 BLOB。返回 null 表示 kb 不存在或无 blob。
     */
    public byte[] readIndexBlob(Long kbId) {
        if (jdbc == null || kbId == null) return null;
        try {
            List<byte[]> rows = jdbc.query(
                    "SELECT index_blob FROM ai_vector_index WHERE kb_id = ?",
                    (rs, i) -> rs.getBytes(1),
                    kbId);
            return rows.isEmpty() ? null : rows.get(0);
        } catch (Exception e) {
            log.error("[VectorIndexPersistence] readIndexBlob failed kb={}", kbId, e);
            return null;
        }
    }

    /**
     * 读取一行完整元数据(用于 loadFromDb 时获取 dim / m / efConstruction / efSearch)。
     * 找不到返回 null。
     */
    public IndexMeta readMeta(Long kbId) {
        if (jdbc == null || kbId == null) return null;
        try {
            List<IndexMeta> rows = jdbc.query(
                    "SELECT kb_id, dimension, element_count, m, ef_construction, ef_search, index_blob " +
                            "FROM ai_vector_index WHERE kb_id = ?",
                    (rs, i) -> new IndexMeta(
                            rs.getLong(1),
                            rs.getInt(2),
                            rs.getInt(3),
                            rs.getInt(4),
                            rs.getInt(5),
                            rs.getInt(6),
                            rs.getBytes(7)),
                    kbId);
            return rows.isEmpty() ? null : rows.get(0);
        } catch (Exception e) {
            log.error("[VectorIndexPersistence] readMeta failed kb={}", kbId, e);
            return null;
        }
    }

    /**
     * upsert 写入索引(同 kb_id 覆盖)。
     */
    public void writeIndexBlob(Long kbId, int dim, int count, int m, int efConstruction, int efSearch, byte[] blob) {
        if (jdbc == null || kbId == null) return;
        try {
            jdbc.update(
                    "INSERT INTO ai_vector_index " +
                            "(kb_id, dimension, element_count, m, ef_construction, ef_search, index_blob) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE " +
                            "dimension=VALUES(dimension), element_count=VALUES(element_count), " +
                            "m=VALUES(m), ef_construction=VALUES(ef_construction), ef_search=VALUES(ef_search), " +
                            "index_blob=VALUES(index_blob)",
                    kbId, dim, count, m, efConstruction, efSearch, blob);
        } catch (Exception e) {
            log.error("[VectorIndexPersistence] writeIndexBlob failed kb={} dim={} count={}",
                    kbId, dim, count, e);
        }
    }

    public List<Long> listAllKbIds() {
        if (jdbc == null) return List.of();
        try {
            return jdbc.query("SELECT kb_id FROM ai_vector_index",
                    (rs, i) -> rs.getLong(1));
        } catch (Exception e) {
            log.error("[VectorIndexPersistence] listAllKbIds failed", e);
            return List.of();
        }
    }

    public void delete(Long kbId) {
        if (jdbc == null || kbId == null) return;
        try {
            jdbc.update("DELETE FROM ai_vector_index WHERE kb_id = ?", kbId);
        } catch (Exception e) {
            log.error("[VectorIndexPersistence] delete failed kb={}", kbId, e);
        }
    }

    public record IndexMeta(Long kbId,
                            int dimension,
                            int elementCount,
                            int m,
                            int efConstruction,
                            int efSearch,
                            byte[] blob) {}
}
