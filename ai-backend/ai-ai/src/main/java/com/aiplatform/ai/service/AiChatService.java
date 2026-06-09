package com.aiplatform.ai.service;

import com.aiplatform.ai.dto.ChatRequest;
import com.aiplatform.ai.dto.ChatResponse;
import com.aiplatform.ai.entity.AiChatMessage;
import com.aiplatform.ai.entity.AiChatSession;
import com.aiplatform.ai.llm.ChatService;
import com.aiplatform.ai.mapper.AiChatMessageMapper;
import com.aiplatform.ai.mapper.AiChatSessionMapper;
import com.aiplatform.common.context.UserContext;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AI 对话服务:同步 + SSE 流式。
 * 简化版 PoC — 真实 RAG/工具调用在 Sprint 2.1 接入。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final ChatService chatService;
    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;

    public AiChatSession createSession(ChatRequest req) {
        Long uid = UserContext.getUserId();
        if (uid == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        AiChatSession s = new AiChatSession();
        s.setUserId(uid);
        s.setProjectId(req.getProjectId());
        s.setTitle(req.getTitle() == null ? "新会话" : req.getTitle());
        s.setModelId(req.getModelId());
        s.setSystemPrompt(req.getSystemPrompt());
        s.setTemperature(req.getTemperature());
        s.setMaxTokens(req.getMaxTokens() == null ? 4096 : req.getMaxTokens());
        s.setKbIds(req.getKbIds());
        s.setMessageCount(0);
        s.setTokenUsed(0L);
        s.setStatus(1);
        s.setPin(0);
        sessionMapper.insert(s);
        return s;
    }

    public AiChatSession getSession(Long id) {
        AiChatSession s = sessionMapper.selectById(id);
        if (s == null) throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        return s;
    }

    public java.util.List<AiChatSession> listMySessions() {
        Long uid = UserContext.getUserId();
        if (uid == null) return java.util.List.of();
        return sessionMapper.selectList(new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getUserId, uid)
                .orderByDesc(AiChatSession::getPin)
                .orderByDesc(AiChatSession::getUpdateTime));
    }

    public java.util.List<AiChatMessage> listMessages(Long sessionId) {
        return messageMapper.selectBySessionId(sessionId);
    }

    @Transactional
    public void deleteSession(Long id) {
        sessionMapper.deleteById(id);
        messageMapper.delete(new LambdaQueryWrapper<AiChatMessage>().eq(AiChatMessage::getSessionId, id));
    }

    @Transactional
    public ChatResponse chat(ChatRequest req) {
        if (req.getSessionId() == null) {
            AiChatSession s = createSession(req);
            req.setSessionId(s.getId());
        }
        Long sessionId = req.getSessionId();
        AiChatSession session = getSession(sessionId);
        Long modelId = req.getModelId() != null ? req.getModelId() : session.getModelId();
        if (modelId == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "未指定模型");

        // 保存 user 消息
        AiChatMessage userMsg = new AiChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(req.getMessage());
        userMsg.setStatus(1);
        long t1 = System.currentTimeMillis();
        messageMapper.insert(userMsg);

        // 调用 LLM
        ChatService.ChatResult result = chatService.chat(modelId,
                req.getSystemPrompt() != null ? req.getSystemPrompt() : session.getSystemPrompt(),
                req.getMessage(),
                req.getTemperature() != null ? req.getTemperature().doubleValue()
                        : (session.getTemperature() == null ? 0.7 : session.getTemperature().doubleValue()),
                req.getMaxTokens() != null ? req.getMaxTokens() : session.getMaxTokens());

        // 保存 assistant 消息
        AiChatMessage aiMsg = new AiChatMessage();
        aiMsg.setSessionId(sessionId);
        aiMsg.setRole("assistant");
        aiMsg.setContent(result.content());
        aiMsg.setInputTokens(result.inputTokens());
        aiMsg.setOutputTokens(result.outputTokens());
        aiMsg.setCostMs((int) (System.currentTimeMillis() - t1));
        aiMsg.setStatus(1);
        messageMapper.insert(aiMsg);

        // 更新会话统计
        session.setMessageCount((session.getMessageCount() == null ? 0 : session.getMessageCount()) + 2);
        long add = (long) (result.inputTokens() == null ? 0 : result.inputTokens())
                + (result.outputTokens() == null ? 0 : result.outputTokens());
        session.setTokenUsed((session.getTokenUsed() == null ? 0L : session.getTokenUsed()) + add);
        sessionMapper.updateById(session);

        ChatResponse resp = new ChatResponse();
        resp.setSessionId(sessionId);
        resp.setMessageId(aiMsg.getId());
        resp.setContent(result.content());
        resp.setInputTokens(result.inputTokens());
        resp.setOutputTokens(result.outputTokens());
        resp.setCostMs(aiMsg.getCostMs());
        return resp;
    }

    /**
     * SSE 流式对话 — Sprint 2 PoC 版。
     * 简化:一次性把完整响应发送出去,带分块延迟(模拟流式)。
     * 真实实现:用 LangChain4j StreamingChatModel + SSE event.
     */
    public SseEmitter streamChat(ChatRequest req) {
        SseEmitter emitter = new SseEmitter(60_000L);
        new Thread(() -> {
            try {
                ChatResponse resp = chat(req);
                if (resp.getContent() == null) {
                    emitter.complete();
                    return;
                }
                String content = resp.getContent();
                int chunkSize = 8;
                for (int i = 0; i < content.length(); i += chunkSize) {
                    int end = Math.min(content.length(), i + chunkSize);
                    String chunk = content.substring(i, end);
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("type", "content");
                    payload.put("data", chunk);
                    payload.put("sessionId", resp.getSessionId());
                    payload.put("messageId", resp.getMessageId());
                    emitter.send(SseEmitter.event().data(payload));
                    Thread.sleep(30);
                }
                Map<String, Object> done = new HashMap<>();
                done.put("type", "done");
                done.put("sessionId", resp.getSessionId());
                done.put("messageId", resp.getMessageId());
                done.put("inputTokens", resp.getInputTokens());
                done.put("outputTokens", resp.getOutputTokens());
                done.put("costMs", resp.getCostMs());
                emitter.send(SseEmitter.event().data(done));
                emitter.complete();
            } catch (IOException | InterruptedException e) {
                emitter.completeWithError(e);
            }
        }, "sse-chat").start();
        return emitter;
    }
}
