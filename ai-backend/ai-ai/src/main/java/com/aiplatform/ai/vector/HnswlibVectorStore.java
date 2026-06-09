package com.aiplatform.ai.vector;

import com.github.jelmerk.hnswlib.core.DistanceFunctions;
import com.github.jelmerk.hnswlib.core.Item;
import com.github.jelmerk.hnswlib.core.SearchResult;
import com.github.jelmerk.hnswlib.core.hnsw.HnswIndex;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Hnswlib 1.2.1 真实向量库封装(替换原内存 ArrayList PoC)。
 *
 * 设计:
 * - 每知识库一个 HnswIndex 实例(在内存)
 * - 维度:create-if-absent 锁住,addItem 时若 dim 不一致抛 IllegalArgumentException
 * - 持久化:VectorIndexPersistence 读/写 ai_vector_index.index_blob BLOB
 * - 启动时遍历表,逐 kb 加载到内存
 * - 写盘:每 100 次 addItem 或 5s(@Scheduled)flush dirty 索引
 * - Hnswlib 参数:M=16, efConstruction=200, efSearch=50(默认),maxItemCount=1_000_000
 *
 * 公开 API 与原 PoC 兼容(方法名/参数/返回类型不变,ScoredResult record 同形)。
 * 新增: addBatch。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HnswlibVectorStore {

    private static final int DEFAULT_M = 16;
    private static final int DEFAULT_EF_CONSTRUCTION = 200;
    private static final int DEFAULT_EF_SEARCH = 50;
    private static final int DEFAULT_MAX_ITEM_COUNT = 1_000_000;
    private static final int FLUSH_THRESHOLD = 100;
    private static final int FLUSH_INTERVAL_MS = 5_000;

    /** 索引内存表(kbId → 索引+dim+dirty 计数)。 */
    private final Map<Long, IndexEntry> indexes = new ConcurrentHashMap<>();

    private final VectorIndexPersistence persistence;

    @PostConstruct
    void init() {
        // 启动时遍历 ai_vector_index,逐 kb 加载到内存
        List<Long> kbIds;
        try {
            kbIds = persistence.listAllKbIds();
        } catch (Exception e) {
            log.warn("[HnswlibVectorStore] listAllKbIds 失败,启动加载跳过: {}", e.getMessage());
            return;
        }
        int ok = 0, fail = 0;
        for (Long kbId : kbIds) {
            try {
                loadFromDb(kbId);
                ok++;
            } catch (Exception e) {
                fail++;
                log.error("[HnswlibVectorStore] 启动加载失败 kb={}", kbId, e);
            }
        }
        log.info("[HnswlibVectorStore] 启动加载完成: total={} ok={} fail={}", kbIds.size(), ok, fail);
    }

    @PreDestroy
    void shutdown() {
        log.info("[HnswlibVectorStore] 关闭,flush 所有 dirty 索引");
        for (Long kbId : new ArrayList<>(indexes.keySet())) {
            try {
                saveToDb(kbId);
            } catch (Exception e) {
                log.error("[HnswlibVectorStore] shutdown flush failed kb={}", kbId, e);
            }
        }
    }

    /**
     * 懒加载/创建 kb 索引。
     * 已有:校验 dim 一致;DB 已有:从 BLOB 反序列化;否则新建。
     */
    public synchronized void initIndex(Long kbId, int dim) {
        if (kbId == null) throw new IllegalArgumentException("kbId is null");
        IndexEntry existing = indexes.get(kbId);
        if (existing != null) {
            if (existing.dimension != dim) {
                throw new IllegalArgumentException(
                        "kb=" + kbId + " 已初始化为 dim=" + existing.dimension + ",新请求 dim=" + dim);
            }
            return;
        }
        // 尝试从 DB 加载
        VectorIndexPersistence.IndexMeta meta = persistence.readMeta(kbId);
        HnswIndex<Long, float[], FloatItem, Float> hnsw;
        if (meta != null && meta.blob() != null && meta.blob().length > 0) {
            if (meta.dimension() != dim) {
                throw new IllegalArgumentException(
                        "kb=" + kbId + " DB 已存 dim=" + meta.dimension() + ",与请求 dim=" + dim + " 不一致");
            }
            hnsw = deserialize(meta.blob());
            log.info("[HnswlibVectorStore] 从 DB 加载索引 kb={} dim={} count={}",
                    kbId, meta.dimension(), hnsw.size());
        } else {
            hnsw = newIndex(dim);
            log.info("[HnswlibVectorStore] 新建空索引 kb={} dim={}", kbId, dim);
        }
        IndexEntry entry = new IndexEntry(hnsw, dim);
        indexes.put(kbId, entry);
    }

    public void addItem(Long kbId, Long vectorId, float[] vector) {
        if (kbId == null || vectorId == null || vector == null) return;
        // 第一次 add 也要保证 initIndex(可能 dim 就是 vector.length)
        if (!indexes.containsKey(kbId)) {
            initIndex(kbId, vector.length);
        }
        IndexEntry entry = indexes.get(kbId);
        if (entry == null) {
            throw new IllegalStateException("initIndex 失败 kb=" + kbId);
        }
        if (entry.dimension != vector.length) {
            throw new IllegalArgumentException(
                    "kb=" + kbId + " dim=" + entry.dimension + " 与 addItem vec.len=" + vector.length + " 不一致");
        }
        FloatItem item = new FloatItem(vectorId, vector);
        entry.index.add(item);
        int d = entry.dirtyCounter.incrementAndGet();
        if (d >= FLUSH_THRESHOLD) {
            try {
                saveToDb(kbId);
            } catch (Exception e) {
                log.warn("[HnswlibVectorStore] 阈值 flush 失败 kb={}: {}", kbId, e.getMessage());
            }
        }
    }

    public void addBatch(Long kbId, List<Long> ids, List<float[]> vectors) {
        if (kbId == null || ids == null || vectors == null) return;
        if (ids.size() != vectors.size()) {
            throw new IllegalArgumentException("ids.size=" + ids.size() + " vectors.size=" + vectors.size());
        }
        if (ids.isEmpty()) return;
        if (!indexes.containsKey(kbId)) {
            initIndex(kbId, vectors.get(0).length);
        }
        IndexEntry entry = indexes.get(kbId);
        if (entry == null) throw new IllegalStateException("initIndex 失败 kb=" + kbId);
        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.get(i);
            float[] v = vectors.get(i);
            if (id == null || v == null) continue;
            if (v.length != entry.dimension) {
                throw new IllegalArgumentException(
                        "kb=" + kbId + " dim=" + entry.dimension + " 与 addBatch vec.len=" + v.length + " 不一致(idx=" + i + ")");
            }
            entry.index.add(new FloatItem(id, v));
        }
        int d = entry.dirtyCounter.addAndGet(ids.size());
        if (d >= FLUSH_THRESHOLD) {
            try {
                saveToDb(kbId);
            } catch (Exception e) {
                log.warn("[HnswlibVectorStore] 批量阈值 flush 失败 kb={}: {}", kbId, e.getMessage());
            }
        }
    }

    public List<ScoredResult> search(Long kbId, float[] query, int topK) {
        IndexEntry entry = indexes.get(kbId);
        if (entry == null || entry.index.size() == 0 || query == null) return List.of();
        if (query.length != entry.dimension) {
            throw new IllegalArgumentException(
                    "kb=" + kbId + " dim=" + entry.dimension + " 与 query.len=" + query.length + " 不一致");
        }
        List<SearchResult<FloatItem, Float>> raw = entry.index.findNearest(query, topK);
        // hnswlib cosine distance: 1 - similarity。score 越大越相关 → 转换: score = 1 - distance
        return raw.stream()
                .map(r -> new ScoredResult(r.item().id, 1f - r.distance()))
                .sorted((a, b) -> Float.compare(b.score, a.score))
                .collect(Collectors.toList());
    }

    public void removeItem(Long kbId, Long vectorId) {
        IndexEntry entry = indexes.get(kbId);
        if (entry == null || vectorId == null) return;
        // hnswlib remove 需要 version;此处用 Long hashCode 作为单调 version
        entry.index.remove(vectorId, vectorId.hashCode());
        entry.dirtyCounter.incrementAndGet();
    }

    public int size(Long kbId) {
        IndexEntry entry = indexes.get(kbId);
        return entry == null ? 0 : entry.index.size();
    }

    public void clear(Long kbId) {
        IndexEntry entry = indexes.remove(kbId);
        if (entry != null) {
            // hnswlib 不支持整体清空;新建一个同 dim 空索引
            try {
                HnswIndex<Long, float[], FloatItem, Float> fresh = newIndex(entry.dimension);
                indexes.put(kbId, new IndexEntry(fresh, entry.dimension));
            } catch (Exception e) {
                log.warn("[HnswlibVectorStore] clear 重建索引失败 kb={}: {}", kbId, e.getMessage());
            }
            persistence.delete(kbId);
        }
    }

    public void clearAll() {
        List<Long> all = new ArrayList<>(indexes.keySet());
        for (Long kbId : all) {
            clear(kbId);
        }
    }

    /**
     * 序列化索引到 BLOB 并 upsert 到 ai_vector_index。
     * 触发:
     *   - addItem/addBatch 计数满 100
     *   - @Scheduled 5s 周期
     *   - shutdown
     */
    public void saveToDb(Long kbId) {
        IndexEntry entry = indexes.get(kbId);
        if (entry == null) return;
        if (entry.dirtyCounter.get() == 0 && entry.index.size() > 0) {
            // 无 dirty 但有数据:首次加载后尚未 flush,直接写一次
        }
        try {
            byte[] blob = serialize(entry.index);
            int size = entry.index.size();
            persistence.writeIndexBlob(
                    kbId,
                    entry.dimension,
                    size,
                    entry.index.getM(),
                    entry.index.getEfConstruction(),
                    entry.index.getEf(),
                    blob);
            entry.dirtyCounter.set(0);
            log.debug("[HnswlibVectorStore] saveToDb kb={} size={} bytes={}", kbId, size, blob.length);
        } catch (Exception e) {
            log.error("[HnswlibVectorStore] saveToDb failed kb={}", kbId, e);
            throw new RuntimeException("saveToDb failed kb=" + kbId, e);
        }
    }

    /**
     * 从 ai_vector_index 加载到内存(若已存在则替换)。
     * 找到但 BLOB 为空:不替换(视为未初始化)。
     */
    public void loadFromDb(Long kbId) {
        VectorIndexPersistence.IndexMeta meta = persistence.readMeta(kbId);
        if (meta == null) return;
        if (meta.blob() == null || meta.blob().length == 0) {
            log.debug("[HnswlibVectorStore] loadFromDb 无 BLOB kb={}", kbId);
            return;
        }
        HnswIndex<Long, float[], FloatItem, Float> hnsw = deserialize(meta.blob());
        IndexEntry entry = new IndexEntry(hnsw, meta.dimension());
        indexes.put(kbId, entry);
        log.info("[HnswlibVectorStore] loadFromDb kb={} dim={} count={}", kbId, meta.dimension(), hnsw.size());
    }

    /**
     * 5s 周期 flush 所有 dirty 索引。
     */
    @Scheduled(fixedDelay = FLUSH_INTERVAL_MS)
    public void scheduledFlush() {
        for (Map.Entry<Long, IndexEntry> e : indexes.entrySet()) {
            if (e.getValue().dirtyCounter.get() > 0) {
                try {
                    saveToDb(e.getKey());
                } catch (Exception ex) {
                    log.warn("[HnswlibVectorStore] scheduled flush failed kb={}: {}", e.getKey(), ex.getMessage());
                }
            }
        }
    }

    // -------- helpers --------

    private HnswIndex<Long, float[], FloatItem, Float> newIndex(int dim) {
        return HnswIndex
                .newBuilder(dim, DistanceFunctions.FLOAT_COSINE_DISTANCE, DEFAULT_MAX_ITEM_COUNT)
                .withM(DEFAULT_M)
                .withEfConstruction(DEFAULT_EF_CONSTRUCTION)
                .withEf(DEFAULT_EF_SEARCH)
                .withRemoveEnabled()
                .build();
    }

    private static byte[] serialize(HnswIndex<Long, float[], FloatItem, Float> index) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(64 * 1024);
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            index.save(oos);
        }
        return baos.toByteArray();
    }

    private static HnswIndex<Long, float[], FloatItem, Float> deserialize(byte[] blob) {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(blob))) {
            return HnswIndex.load(ois);
        } catch (IOException e) {
            throw new RuntimeException("HnswIndex 反序列化失败", e);
        }
    }

    /**
     * 内部索引项(Long id + float[] vector)。implements Item + Serializable。
     */
    public static final class FloatItem implements Item<Long, float[]>, Serializable {
        private static final long serialVersionUID = 1L;
        final Long id;
        final float[] vector;

        FloatItem(Long id, float[] vector) {
            this.id = id;
            this.vector = vector;
        }

        @Override
        public Long id() {
            return id;
        }

        @Override
        public float[] vector() {
            return vector;
        }

        @Override
        public int dimensions() {
            return vector.length;
        }
    }

    /**
     * 索引条目(在 ConcurrentHashMap 中保存)。
     */
    private static final class IndexEntry {
        final HnswIndex<Long, float[], FloatItem, Float> index;
        final int dimension;
        final AtomicInteger dirtyCounter = new AtomicInteger(0);

        IndexEntry(HnswIndex<Long, float[], FloatItem, Float> index, int dimension) {
            this.index = index;
            this.dimension = dimension;
        }
    }

    /**
     * 对外暴露的 record(与原 PoC 兼容)。
     */
    public record ScoredResult(Long vectorId, float score) {}
}
