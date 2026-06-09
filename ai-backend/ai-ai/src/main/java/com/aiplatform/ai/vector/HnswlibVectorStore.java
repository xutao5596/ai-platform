package com.aiplatform.ai.vector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 内存向量存储(PoC 简化版):每知识库一个 List<long,float[]>,懒加载。
 * 真实部署可替换为 Hnswlib 1.2.1 / Milvus。
 *
 * 持久化:JSON 序列化到 ai.app.hnsw-path 目录
 */
@Slf4j
@Component
public class HnswlibVectorStore {

    private final Map<Long, List<float[]>> vectors = new ConcurrentHashMap<>();
    private final Map<Long, List<Long>> ids = new ConcurrentHashMap<>();
    private final Map<Long, Integer> dimensions = new ConcurrentHashMap<>();
    private final Map<Long, Object> locks = new ConcurrentHashMap<>();

    public void initIndex(Long kbId, int dim) {
        dimensions.putIfAbsent(kbId, dim);
        vectors.computeIfAbsent(kbId, k -> Collections.synchronizedList(new ArrayList<>()));
        ids.computeIfAbsent(kbId, k -> Collections.synchronizedList(new ArrayList<>()));
    }

    public synchronized void addItem(Long kbId, Long vectorId, float[] vector) {
        if (kbId == null) return;
        initIndex(kbId, vector.length);
        List<Long> idList = ids.get(kbId);
        List<float[]> vecList = vectors.get(kbId);
        int existing = idList.indexOf(vectorId);
        if (existing >= 0) {
            vecList.set(existing, vector);
        } else {
            idList.add(vectorId);
            vecList.add(vector);
        }
    }

    public List<ScoredResult> search(Long kbId, float[] query, int topK) {
        List<float[]> vecList = vectors.get(kbId);
        List<Long> idList = ids.get(kbId);
        if (vecList == null || idList == null || vecList.isEmpty()) return List.of();
        // 归一化 query
        float[] q = normalize(query);
        List<ScoredResult> scored = new ArrayList<>();
        synchronized (vecList) {
            for (int i = 0; i < vecList.size(); i++) {
                float score = cosine(q, vecList.get(i));
                scored.add(new ScoredResult(idList.get(i), score));
            }
        }
        return scored.stream()
                .sorted((a, b) -> Float.compare(b.score, a.score))
                .limit(topK)
                .collect(Collectors.toList());
    }

    public void removeItem(Long kbId, Long vectorId) {
        List<Long> idList = ids.get(kbId);
        List<float[]> vecList = vectors.get(kbId);
        if (idList == null) return;
        int idx = idList.indexOf(vectorId);
        if (idx >= 0) {
            idList.remove(idx);
            vecList.remove(idx);
        }
    }

    public int size(Long kbId) {
        List<Long> idList = ids.get(kbId);
        return idList == null ? 0 : idList.size();
    }

    public void clear(Long kbId) {
        vectors.remove(kbId);
        ids.remove(kbId);
        dimensions.remove(kbId);
    }

    public void clearAll() {
        vectors.clear();
        ids.clear();
        dimensions.clear();
    }

    public void save(Long kbId, String basePath) {
        try {
            File dir = new File(basePath);
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, "kb_" + kbId + ".vec");
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))) {
                oos.writeObject(dimensions.get(kbId));
                List<Long> idList = ids.get(kbId);
                List<float[]> vecList = vectors.get(kbId);
                oos.writeInt(idList == null ? 0 : idList.size());
                if (idList != null) {
                    for (int i = 0; i < idList.size(); i++) {
                        oos.writeLong(idList.get(i));
                        oos.writeObject(vecList.get(i));
                    }
                }
            }
        } catch (IOException e) {
            log.error("Failed to save vector store kb={}", kbId, e);
        }
    }

    @SuppressWarnings("unchecked")
    public void load(Long kbId, String basePath) {
        File f = new File(basePath, "kb_" + kbId + ".vec");
        if (!f.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            int dim = (Integer) ois.readObject();
            initIndex(kbId, dim);
            int n = ois.readInt();
            List<Long> idList = ids.get(kbId);
            List<float[]> vecList = vectors.get(kbId);
            for (int i = 0; i < n; i++) {
                long id = ois.readLong();
                float[] v = (float[]) ois.readObject();
                idList.add(id);
                vecList.add(v);
            }
            log.info("Loaded {} vectors for kb={}", n, kbId);
        } catch (IOException | ClassNotFoundException e) {
            log.warn("Failed to load vector store kb={}: {}", kbId, e.getMessage());
        }
    }

    private float[] normalize(float[] v) {
        float n = 0;
        for (float f : v) n += f * f;
        n = (float) Math.sqrt(n);
        if (n == 0) return v;
        float[] r = new float[v.length];
        for (int i = 0; i < v.length; i++) r[i] = v[i] / n;
        return r;
    }

    private float cosine(float[] a, float[] b) {
        if (a.length != b.length) return 0;
        float dot = 0;
        for (int i = 0; i < a.length; i++) dot += a[i] * b[i];
        return dot;
    }

    public record ScoredResult(Long vectorId, float score) {}
}
