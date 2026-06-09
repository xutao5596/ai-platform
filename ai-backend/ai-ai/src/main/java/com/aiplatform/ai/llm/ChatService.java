package com.aiplatform.ai.llm;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.output.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 统一 Chat 服务(同步 + 流式)。封装 LangChain4j 调用,供 ChatController / FlowNode 复用。
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

    public record ChatResult(String content, Integer inputTokens, Integer outputTokens) {}
}
