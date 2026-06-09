package com.aiplatform.ai.llm;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 统一 Chat 服务(同步 + 流式 + 工具调用 + embedding)。封装 LangChain4j 调用,供 ChatController / FlowNode / Assistant 复用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final LlmProviderFactory factory;

    public ChatResult chat(Long modelId, String systemPrompt, String userMessage) {
        return chat(modelId, systemPrompt, userMessage, 0.7, 4096);
    }

    public ChatResult chat(Long modelId, String systemPrompt, String userMessage, Double temperature, Integer maxTokens) {
        var model = factory.getChatModel(modelId);
        List<ChatMessage> msgs = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            msgs.add(SystemMessage.from(systemPrompt));
        }
        msgs.add(UserMessage.from(userMessage));
        ChatResponse resp = model.chat(msgs);
        return new ChatResult(
                resp.aiMessage() == null ? "" : resp.aiMessage().text(),
                resp.tokenUsage() == null ? null : resp.tokenUsage().inputTokenCount(),
                resp.tokenUsage() == null ? null : resp.tokenUsage().outputTokenCount()
        );
    }

    /**
     * Token-by-token 流式调用(契约方法:单轮,无工具)。handler.onToken 每次可能拿到 1+ token(provider batching),
     * onComplete 返回 StreamResult(包含累积文本 + 工具调用 + token usage)。
     * 工具循环请使用 {@link #callWithTools}。
     */
    public void stream(Long modelId, String systemPrompt, String userMessage,
                       Double temperature, Integer maxTokens, TokenHandler handler) {
        streamInternal(modelId, systemPrompt, userMessage, null, null, temperature, maxTokens, handler);
    }

    /**
     * 流式调用(带 history + tools,首轮工具调用场景)。完成后用 handler.onCompleteWithResult 返回完整结果。
     */
    public void stream(Long modelId, String systemPrompt, List<ChatMessage> history,
                       List<ToolSpecification> tools,
                       Double temperature, Integer maxTokens, TokenHandler handler) {
        streamInternal(modelId, systemPrompt, null, history, tools, temperature, maxTokens, handler);
    }

    private void streamInternal(Long modelId, String systemPrompt, String userMessage,
                                List<ChatMessage> history, List<ToolSpecification> tools,
                                Double temperature, Integer maxTokens, TokenHandler handler) {
        StreamingChatModel model = factory.getStreamingChatModel(modelId);
        List<ChatMessage> msgs = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            msgs.add(SystemMessage.from(systemPrompt));
        }
        if (history != null) {
            msgs.addAll(history);
        } else if (userMessage != null) {
            msgs.add(UserMessage.from(userMessage));
        }
        OpenAiChatRequestParameters.Builder params = OpenAiChatRequestParameters.builder()
                .temperature(temperature == null ? 0.7 : temperature)
                .maxOutputTokens(maxTokens == null ? 4096 : maxTokens);
        if (tools != null && !tools.isEmpty()) {
            params.toolSpecifications(tools);
        }
        ChatRequest req = ChatRequest.builder()
                .messages(msgs)
                .parameters(params.build())
                .build();

        java.util.concurrent.atomic.AtomicReference<TokenUsage> usageRef = new java.util.concurrent.atomic.AtomicReference<>();
        StringBuilder textRef = new StringBuilder();
        List<ToolCallRequest> toolCalls = java.util.Collections.synchronizedList(new ArrayList<>());

        model.chat(req, new dev.langchain4j.model.chat.response.StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                if (partialResponse != null && !partialResponse.isEmpty()) {
                    synchronized (textRef) {
                        textRef.append(partialResponse);
                    }
                    try {
                        handler.onToken(partialResponse);
                    } catch (Exception ex) {
                        log.warn("TokenHandler.onToken 抛出,忽略: {}", ex.getMessage());
                    }
                }
            }
            @Override
            public void onCompleteToolCall(dev.langchain4j.model.chat.response.CompleteToolCall completeToolCall) {
                ToolExecutionRequest ter = completeToolCall.toolExecutionRequest();
                toolCalls.add(new ToolCallRequest(ter.id(), ter.name(), ter.arguments()));
            }
            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                if (completeResponse != null && completeResponse.tokenUsage() != null) {
                    usageRef.set(completeResponse.tokenUsage());
                }
                String text;
                synchronized (textRef) {
                    text = textRef.toString();
                }
                StreamResult sr = new StreamResult(
                        text,
                        toolCalls,
                        usageRef.get() == null ? null : usageRef.get().inputTokenCount(),
                        usageRef.get() == null ? null : usageRef.get().outputTokenCount());
                handler.onCompleteWithResult(sr);
            }
            @Override
            public void onError(Throwable error) {
                handler.onError(error);
            }
        });
    }

    private List<ChatMessage> buildMessages(String systemPrompt, List<ChatMessage> history) {
        List<ChatMessage> msgs = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            msgs.add(SystemMessage.from(systemPrompt));
        }
        if (history != null) {
            msgs.addAll(history);
        }
        return msgs;
    }

    /**
     * Function Calling:把 history(已含 user/assistant/tool)发给模型,返回是否触发工具调用及结果。
     * 一次调用,不做循环(由调用方在 hasToolCall 时把工具结果追加到 history 再回调)。
     */
    public ToolCallResult callWithTools(Long modelId, String systemPrompt, List<ChatMessage> history,
                                        List<ToolSpecification> tools,
                                        Double temperature, Integer maxTokens) {
        List<ChatMessage> msgs = buildMessages(systemPrompt, history);

        var model = factory.getChatModel(modelId);
        OpenAiChatRequestParameters.Builder params = OpenAiChatRequestParameters.builder()
                .temperature(temperature == null ? 0.7 : temperature)
                .maxOutputTokens(maxTokens == null ? 4096 : maxTokens);
        if (tools != null && !tools.isEmpty()) {
            params.toolSpecifications(tools);
        }
        ChatRequest req = ChatRequest.builder()
                .messages(msgs)
                .parameters(params.build())
                .build();
        ChatResponse resp = model.chat(req);
        AiMessage ai = resp.aiMessage();
        List<ToolCallRequest> calls = new ArrayList<>();
        if (ai != null && ai.hasToolExecutionRequests()) {
            for (ToolExecutionRequest ter : ai.toolExecutionRequests()) {
                calls.add(new ToolCallRequest(ter.id(), ter.name(), ter.arguments()));
            }
        }
        return new ToolCallResult(
                ai == null ? null : ai.text(),
                calls,
                resp.tokenUsage() == null ? null : resp.tokenUsage().inputTokenCount(),
                resp.tokenUsage() == null ? null : resp.tokenUsage().outputTokenCount()
        );
    }

    /**
     * 真实 LLM embedding。dimension 与目标向量库维度匹配(OpenAI 兼容,text-embedding-3-* 支持自定义维度)。
     * 失败抛异常(由调用方 EmbeddingService 兜底零向量)。
     */
    public float[] embed(String text, int dimension) {
        var model = factory.getEmbeddingModel(null, dimension);
        Response<Embedding> resp = model.embed(text == null ? "" : text);
        float[] vec = resp.content().vector();
        if (vec.length != dimension) {
            log.debug("Embedding 维度与请求不一致: requested={}, actual={}", dimension, vec.length);
        }
        return vec;
    }

    public interface TokenHandler {
        void onToken(String token);
        default void onComplete(TokenUsage usage) {}
        default void onCompleteWithResult(StreamResult result) {}
        default void onError(Throwable error) { throw new RuntimeException(error); }
    }

    public record ChatResult(String content, Integer inputTokens, Integer outputTokens) {}

    public record ToolCallResult(String textContent, List<ToolCallRequest> toolCalls,
                                 Integer inputTokens, Integer outputTokens) {
        public boolean hasToolCall() { return toolCalls != null && !toolCalls.isEmpty(); }
    }

    public record ToolCallRequest(String callId, String name, String argumentsJson) {}

    public record StreamResult(String text, List<ToolCallRequest> toolCalls,
                               Integer inputTokens, Integer outputTokens) {
        public boolean hasToolCall() { return toolCalls != null && !toolCalls.isEmpty(); }
    }
}
