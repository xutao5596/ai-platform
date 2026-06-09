package com.aiplatform.ai.vector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 嵌入服务:简化版 — 调用 OpenAI 兼容的 embedding 接口(Sprint 2 完整实现)。
 * Sprint 2 占位:返回零向量(用于演示 Hnswlib 存储路径)。
 * Sprint 2.1 将替换为真实 embedding 调用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    /**
     * 文本转向量。简化实现:返回固定维度零向量(用于 PoC)。
     * 真实实现:调用配置的 embedding 模型 API。
     */
    public float[] embed(String text, int dimension) {
        // 简化 hash → 浮点向量(确定性的,Sprint 2 PoC 用)
        float[] v = new float[dimension];
        int seed = text == null ? 0 : text.hashCode();
        java.util.Random r = new java.util.Random(seed);
        for (int i = 0; i < dimension; i++) {
            v[i] = (float) (r.nextGaussian() * 0.1);
        }
        // 归一化
        float norm = 0;
        for (float f : v) norm += f * f;
        norm = (float) Math.sqrt(norm);
        if (norm > 0) for (int i = 0; i < dimension; i++) v[i] /= norm;
        return v;
    }

    public List<float[]> embedBatch(List<String> texts, int dimension) {
        return texts.stream().map(t -> embed(t, dimension)).toList();
    }
}
