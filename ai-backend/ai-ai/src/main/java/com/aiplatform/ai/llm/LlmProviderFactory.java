package com.aiplatform.ai.llm;

import com.aiplatform.ai.entity.AiModel;
import com.aiplatform.ai.mapper.AiModelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LLM 工厂:基于 ai_model 配置创建 ChatModel / StreamingChatModel / EmbeddingModel 缓存。
 * OpenAI 兼容协议(OpenAI / DeepSeek / 智谱 / 通义 / Ollama / Claude 代理)统一用 OpenAI 客户端。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmProviderFactory {

    private final AiModelMapper modelMapper;
    private final Map<Long, ChatModel> chatCache = new ConcurrentHashMap<>();
    private final Map<Long, StreamingChatModel> streamCache = new ConcurrentHashMap<>();
    private final Map<String, EmbeddingModel> embeddingCache = new ConcurrentHashMap<>();

    public ChatModel getChatModel(Long modelId) {
        return chatCache.computeIfAbsent(modelId, this::buildChat);
    }

    public StreamingChatModel getStreamingChatModel(Long modelId) {
        return streamCache.computeIfAbsent(modelId, this::buildStreaming);
    }

    /**
     * 构造/获取 EmbeddingModel。modelId 为空时使用内置默认(text-embedding-3-small,1536 维)。
     * dimension 非空时通过 OpenAI 兼容协议传给上游(text-embedding-3-* 支持自定义维度)。
     */
    public EmbeddingModel getEmbeddingModel(Long modelId, Integer dimension) {
        String key = (modelId == null ? "default" : String.valueOf(modelId)) + ":" + (dimension == null ? "auto" : dimension);
        return embeddingCache.computeIfAbsent(key, k -> buildEmbedding(modelId, dimension));
    }

    public void invalidate(Long modelId) {
        chatCache.remove(modelId);
        streamCache.remove(modelId);
        embeddingCache.entrySet().removeIf(e -> e.getKey().startsWith(String.valueOf(modelId) + ":"));
    }

    public void invalidateAll() {
        chatCache.clear();
        streamCache.clear();
        embeddingCache.clear();
    }

    public AiModel requireModel(Long modelId) {
        if (modelId == null) {
            throw new IllegalArgumentException("modelId 不能为空");
        }
        AiModel m = modelMapper.selectById(modelId);
        if (m == null) {
            throw new IllegalArgumentException("模型不存在: " + modelId);
        }
        if (Integer.valueOf(0).equals(m.getStatus())) {
            throw new IllegalArgumentException("模型已停用: " + m.getName());
        }
        return m;
    }

    private ChatModel buildChat(Long modelId) {
        AiModel m = requireModel(modelId);
        return OpenAiChatModel.builder()
                .baseUrl(resolveBaseUrl(m))
                .apiKey(m.getApiKey())
                .modelName(m.getModelName())
                .temperature(m.getTemperature() == null ? 0.7 : m.getTemperature().doubleValue())
                .maxTokens(m.getMaxTokens() == null ? 4096 : m.getMaxTokens())
                .timeout(Duration.ofSeconds(120))
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    private StreamingChatModel buildStreaming(Long modelId) {
        AiModel m = requireModel(modelId);
        return OpenAiStreamingChatModel.builder()
                .baseUrl(resolveBaseUrl(m))
                .apiKey(m.getApiKey())
                .modelName(m.getModelName())
                .temperature(m.getTemperature() == null ? 0.7 : m.getTemperature().doubleValue())
                .maxTokens(m.getMaxTokens() == null ? 4096 : m.getMaxTokens())
                .timeout(Duration.ofSeconds(120))
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    private EmbeddingModel buildEmbedding(Long modelId, Integer dimension) {
        if (modelId == null) {
            return OpenAiEmbeddingModel.builder()
                    .baseUrl("https://api.openai.com/v1")
                    .apiKey(System.getenv("OPENAI_API_KEY"))
                    .modelName("text-embedding-3-small")
                    .dimensions(dimension)
                    .timeout(Duration.ofSeconds(60))
                    .logRequests(false)
                    .logResponses(false)
                    .build();
        }
        AiModel m = requireModel(modelId);
        String modelName = m.getEmbeddingModel() != null && !m.getEmbeddingModel().isBlank()
                ? m.getEmbeddingModel()
                : "text-embedding-3-small";
        Integer dim = dimension != null ? dimension : m.getDimension();
        return OpenAiEmbeddingModel.builder()
                .baseUrl(resolveBaseUrl(m))
                .apiKey(m.getApiKey())
                .modelName(modelName)
                .dimensions(dim)
                .timeout(Duration.ofSeconds(60))
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    private String resolveBaseUrl(AiModel m) {
        if (m.getApiBase() != null && !m.getApiBase().isBlank()) {
            return m.getApiBase();
        }
        return switch (String.valueOf(m.getProvider()).toLowerCase()) {
            case "deepseek" -> "https://api.deepseek.com/v1";
            case "qwen" -> "https://dashscope.aliyuncs.com/compatible-mode/v1";
            case "glm" -> "https://open.bigmodel.cn/api/paas/v4";
            case "ollama" -> "http://localhost:11434/v1";
            default -> "https://api.openai.com/v1";
        };
    }
}
