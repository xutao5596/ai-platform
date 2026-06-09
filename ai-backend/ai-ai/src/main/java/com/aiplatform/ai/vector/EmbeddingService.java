package com.aiplatform.ai.vector;

import com.aiplatform.ai.llm.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 嵌入服务:调用真实 LLM embedding API(OpenAI 兼容 /v1/embeddings)。
 * 失败 fallback:返回零向量(不抛异常,保证 Hnswlib 写入路径不被打断)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final ChatService chatService;

    /**
     * 文本转向量。失败时 log warn + 返回 dimension 维零向量(不抛异常)。
     */
    public float[] embed(String text, int dimension) {
        try {
            return chatService.embed(text, dimension);
        } catch (Exception e) {
            log.warn("[EmbeddingService] LLM embedding 失败,返回零向量: dim={}, err={}", dimension, e.getMessage());
            return new float[dimension];
        }
    }

    public List<float[]> embedBatch(List<String> texts, int dimension) {
        return texts.stream().map(t -> embed(t, dimension)).toList();
    }
}
