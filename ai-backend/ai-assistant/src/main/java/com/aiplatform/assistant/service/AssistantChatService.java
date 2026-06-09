package com.aiplatform.assistant.service;

import com.aiplatform.ai.llm.ChatService;
import com.aiplatform.ai.llm.ToolSpecBuilder;
import com.aiplatform.assistant.dto.AssistantChatRequest;
import com.aiplatform.ai.entity.AiAssistantConfig;
import com.aiplatform.assistant.entity.AiAssistantMessage;
import com.aiplatform.assistant.entity.AiAssistantSession;
import com.aiplatform.assistant.entity.AiAssistantToolLog;
import com.aiplatform.ai.mapper.AiAssistantConfigMapper;
import com.aiplatform.assistant.mapper.AiAssistantMessageMapper;
import com.aiplatform.assistant.mapper.AiAssistantSessionMapper;
import com.aiplatform.assistant.mapper.AiAssistantToolLogMapper;
import com.aiplatform.assistant.tools.AssistantTool;
import com.aiplatform.assistant.tools.ToolContext;
import com.aiplatform.assistant.tools.ToolRegistry;
import com.aiplatform.assistant.tools.ToolResult;
import com.aiplatform.common.context.UserContext;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import com.aiplatform.common.util.JsonUtils;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 助手对话服务:基于 LangChain4j Function Calling 的多轮工具循环。
 * SSE 流式:token-by-token 推 content 事件,不再 Thread.sleep 分块。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantChatService {

    /** 最大工具调用轮次(防死循环) */
    private static final int MAX_TOOL_ROUNDS = 5;

    private final AssistantService assistantService;
    private final AiAssistantSessionMapper sessionMapper;
    private final AiAssistantMessageMapper messageMapper;
    private final AiAssistantToolLogMapper toolLogMapper;
    private final AiAssistantConfigMapper configMapper;
    private final ToolRegistry toolRegistry;
    private final ChatService chatService;
    private final ToolSpecBuilder toolSpecBuilder = new ToolSpecBuilder();

    /**
     * 同步对话(返回最终内容,用于测试)。
     */
    public ChatOutcome chat(Long assistantId, AssistantChatRequest req) {
        AiAssistantConfig config = assistantService.get(assistantId);
        Long sessionId = ensureSession(config, req);
        if (req.getMessage() == null || req.getMessage().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "message 不能为空");
        }
        AiAssistantSession session = sessionMapper.selectById(sessionId);
        saveUserMessage(sessionId, req.getMessage());

        long t0 = System.currentTimeMillis();
        List<ChatMessage> history = buildHistory(sessionId);
        AtomicInteger inputTokensTotal = new AtomicInteger();
        AtomicInteger outputTokensTotal = new AtomicInteger();
        List<Map<String, Object>> toolCallsRecord = new ArrayList<>();
        StringBuilder finalContent = new StringBuilder();
        boolean toolsEnabled = req.getToolsEnabled() == null ? true : req.getToolsEnabled();
        List<ToolSpecification> tools = toolsEnabled ? buildToolSpecs() : List.of();
        ToolContext ctx = new ToolContext(assistantId, sessionId,
                UserContext.getUserId(), config.getProjectId());

        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            ChatService.ToolCallResult r = chatService.callWithTools(
                    config.getModelId(),
                    config.getPersona(),
                    history,
                    tools,
                    config.getTemperature() == null ? 0.7 : config.getTemperature().doubleValue(),
                    4096);
            if (r.inputTokens() != null) inputTokensTotal.addAndGet(r.inputTokens());
            if (r.outputTokens() != null) outputTokensTotal.addAndGet(r.outputTokens());

            if (r.hasToolCall()) {
                for (ChatService.ToolCallRequest tc : r.toolCalls()) {
                    String argsJson = tc.argumentsJson() == null ? "{}" : tc.argumentsJson();
                    Map<String, Object> args = parseArgs(argsJson);
                    Map<String, Object> rec = new HashMap<>();
                    rec.put("name", tc.name());
                    rec.put("args", args);
                    rec.put("callId", tc.callId());
                    toolCallsRecord.add(rec);
                    long t1 = System.currentTimeMillis();
                    ToolResult tr = executeTool(tc.name(), args, ctx);
                    int cost = (int) (System.currentTimeMillis() - t1);
                    logTool(assistantId, sessionId, tc, tr, cost);

                    history.add(AiMessage.from(r.textContent(), List.of(ToolExecutionRequest.builder()
                            .id(tc.callId())
                            .name(tc.name())
                            .arguments(argsJson)
                            .build())));
                    history.add(ToolExecutionResultMessage.from(tc.callId(), tc.name(),
                            tr.getOutput() == null ? (tr.getError() == null ? "" : tr.getError()) : tr.getOutput()));
                }
                continue;
            }
            finalContent.append(r.textContent() == null ? "" : r.textContent());
            saveAssistantMessage(sessionId, r.textContent(),
                    toolCallsRecord.isEmpty() ? null : JsonUtils.toJson(toolCallsRecord),
                    inputTokensTotal.get(), outputTokensTotal.get(),
                    (int) (System.currentTimeMillis() - t0));
            session.setMessageCount((session.getMessageCount() == null ? 0 : session.getMessageCount()) + 2);
            session.setUpdateTime(LocalDateTime.now());
            sessionMapper.updateById(session);
            return new ChatOutcome(sessionId, finalContent.toString(),
                    inputTokensTotal.get(), outputTokensTotal.get(),
                    (int) (System.currentTimeMillis() - t0), toolCallsRecord);
        }
        log.warn("Assistant chat exceeded max tool rounds: assistantId={}, sessionId={}", assistantId, sessionId);
        return new ChatOutcome(sessionId, finalContent.toString() + "\n[已达最大工具调用轮次]",
                inputTokensTotal.get(), outputTokensTotal.get(),
                (int) (System.currentTimeMillis() - t0), toolCallsRecord);
    }

    /**
     * SSE 流式对话(基于 LangChain4j Function Calling + StreamingChatModel)。
     * 事件:
     *  - meta       → {"sessionId":1,"messageId":42}
     *  - content    → {"chunk":"<token>","sessionId":1}
     *  - tool_call  → {"name":"...","args":{...},"callId":"..."}
     *  - tool_result→ {"callId":"...","success":true,"output":"..."}
     *  - done       → {"sessionId":1,"messageId":42,"inputTokens":120,"outputTokens":85,"costMs":1234}
     *  - error      → {"message":"..."}
     */
    public SseEmitter streamChat(Long assistantId, AssistantChatRequest req) {
        SseEmitter emitter = new SseEmitter(180_000L);
        com.aiplatform.common.context.LoginUser loginUser = com.aiplatform.common.context.UserContext.get();
        Thread t = new Thread(() -> {
            try {
                if (loginUser != null) {
                    com.aiplatform.common.context.UserContext.set(loginUser);
                }
                AiAssistantConfig config = assistantService.get(assistantId);
                Long sessionId = ensureSession(config, req);
                AiAssistantSession session = sessionMapper.selectById(sessionId);
                AiAssistantMessage userMsg = saveUserMessage(sessionId, req.getMessage());

                emitter.send(SseEmitter.event().name("meta").data(Map.of("sessionId", sessionId, "messageId", userMsg.getId())));

                long t0 = System.currentTimeMillis();
                List<ChatMessage> history = buildHistory(sessionId);
                AtomicInteger inputTokensTotal = new AtomicInteger();
                AtomicInteger outputTokensTotal = new AtomicInteger();
                List<Map<String, Object>> toolCallsRecord = new ArrayList<>();
                StringBuilder finalContent = new StringBuilder();
                boolean toolsEnabled = req.getToolsEnabled() == null ? true : req.getToolsEnabled();
                List<ToolSpecification> tools = toolsEnabled ? buildToolSpecs() : List.of();
                ToolContext ctx = new ToolContext(assistantId, sessionId,
                        UserContext.getUserId(), config.getProjectId());

                Long finalMessageId = null;
                for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
                    // 用流式 + tools 一次调用,handler 推 content 事件;onCompleteWithResult 暴露 tool calls
                    StringBuilder roundText = new StringBuilder();
                    List<ChatService.ToolCallRequest>[] roundToolCalls = new List[]{new ArrayList<>()};
                    AtomicReference<Throwable> roundError = new AtomicReference<>();
                    ChatService.TokenHandler handler = new ChatService.TokenHandler() {
                        @Override
                        public void onToken(String token) {
                            if (token == null || token.isEmpty()) return;
                            roundText.append(token);
                            try {
                                emitter.send(SseEmitter.event().name("content").data(Map.of("chunk", token, "sessionId", sessionId)));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        @Override
                        public void onCompleteWithResult(ChatService.StreamResult result) {
                            if (result != null && result.toolCalls() != null) {
                                roundToolCalls[0] = result.toolCalls();
                            }
                        }
                        @Override
                        public void onError(Throwable error) {
                            roundError.set(error);
                        }
                    };
                    chatService.stream(config.getModelId(), config.getPersona(), history, tools,
                            config.getTemperature() == null ? 0.7 : config.getTemperature().doubleValue(),
                            4096, handler);
                    if (roundError.get() != null) throw roundError.get();

                    finalContent.append(roundText.toString());
                    List<ChatService.ToolCallRequest> tcs = roundToolCalls[0] == null ? List.of() : roundToolCalls[0];

                    if (!tcs.isEmpty()) {
                        // 当前轮的 AI message(含 tool calls)加入 history
                        String currentAiText = roundText.toString();
                        List<ToolExecutionRequest> ters = new ArrayList<>();
                        for (ChatService.ToolCallRequest tc : tcs) {
                            ters.add(ToolExecutionRequest.builder()
                                    .id(tc.callId())
                                    .name(tc.name())
                                    .arguments(tc.argumentsJson() == null ? "{}" : tc.argumentsJson())
                                    .build());
                        }
                        // 1) 把模型发起的 AiMessage(可能含 text 和 tool calls)加入 history
                        if (tcs.size() == 1) {
                            history.add(AiMessage.from(currentAiText.isEmpty() ? null : currentAiText, ters));
                        } else {
                            history.add(AiMessage.from(currentAiText.isEmpty() ? null : currentAiText, ters));
                        }
                        // 2) 对每个 tool_call:推送 SSE 事件 + 执行 + 推 tool_result + 追加 ToolExecutionResultMessage
                        for (ChatService.ToolCallRequest tc : tcs) {
                            String argsJson = tc.argumentsJson() == null ? "{}" : tc.argumentsJson();
                            Map<String, Object> args = parseArgs(argsJson);
                            Map<String, Object> rec = new HashMap<>();
                            rec.put("name", tc.name());
                            rec.put("args", args);
                            rec.put("callId", tc.callId());
                            toolCallsRecord.add(rec);

                            Map<String, Object> tcEvt = new HashMap<>();
                            tcEvt.put("name", tc.name());
                            tcEvt.put("args", args);
                            tcEvt.put("callId", tc.callId());
                            emitter.send(SseEmitter.event().name("tool_call").data(tcEvt));

                            long t1 = System.currentTimeMillis();
                            ToolResult tr = executeTool(tc.name(), args, ctx);
                            int cost = (int) (System.currentTimeMillis() - t1);
                            logTool(assistantId, sessionId, tc, tr, cost);

                            Map<String, Object> trEvt = new HashMap<>();
                            trEvt.put("callId", tc.callId());
                            trEvt.put("success", tr.isSuccess());
                            trEvt.put("output", tr.getOutput() == null ? "" : tr.getOutput());
                            if (tr.getError() != null) trEvt.put("error", tr.getError());
                            emitter.send(SseEmitter.event().name("tool_result").data(trEvt));

                            history.add(ToolExecutionResultMessage.from(tc.callId(), tc.name(),
                                    tr.getOutput() == null ? (tr.getError() == null ? "" : tr.getError()) : tr.getOutput()));
                        }
                        continue;
                    }

                    // 工具 token usage 估算(本轮 LLM 调用);流式 API 在 onCompleteWithResult 里返回 usage
                    // 这里我们没有从 onCompleteWithResult 取出,简化用 null,统计由首轮 stream 一次完成
                    AiAssistantMessage aiMsg = saveAssistantMessage(sessionId, finalContent.toString(),
                            toolCallsRecord.isEmpty() ? null : JsonUtils.toJson(toolCallsRecord),
                            inputTokensTotal.get(), outputTokensTotal.get(),
                            (int) (System.currentTimeMillis() - t0));
                    finalMessageId = aiMsg.getId();
                    session.setMessageCount((session.getMessageCount() == null ? 0 : session.getMessageCount()) + 2);
                    session.setUpdateTime(LocalDateTime.now());
                    sessionMapper.updateById(session);
                    Map<String, Object> done = new HashMap<>();
                    done.put("sessionId", sessionId);
                    done.put("messageId", finalMessageId);
                    done.put("inputTokens", inputTokensTotal.get());
                    done.put("outputTokens", outputTokensTotal.get());
                    done.put("costMs", (int) (System.currentTimeMillis() - t0));
                    emitter.send(SseEmitter.event().name("done").data(done));
                    emitter.complete();
                    return;
                }
                emitter.send(SseEmitter.event().name("content").data(Map.of("chunk", "\n[已达最大工具调用轮次]", "sessionId", sessionId)));
                Map<String, Object> done = new HashMap<>();
                done.put("sessionId", sessionId);
                done.put("messageId", finalMessageId);
                done.put("inputTokens", inputTokensTotal.get());
                done.put("outputTokens", outputTokensTotal.get());
                done.put("costMs", (int) (System.currentTimeMillis() - t0));
                done.put("warning", "max_tool_rounds_reached");
                emitter.send(SseEmitter.event().name("done").data(done));
                emitter.complete();
            } catch (Throwable ex) {
                log.error("Assistant stream error", ex);
                try {
                    emitter.send(SseEmitter.event().name("error").data(Map.of("message", ex.getMessage() == null ? ex.toString() : ex.getMessage())));
                } catch (IOException ignored) {
                }
                emitter.completeWithError(ex);
            } finally {
                com.aiplatform.common.context.UserContext.clear();
            }
        }, "assistant-sse-" + assistantId + "-" + System.currentTimeMillis());
        t.setDaemon(true);
        t.start();
        return emitter;
    }

    /**
     * 用 ToolRegistry 生成 LangChain4j ToolSpecification 列表(避免 ai-ai 直接引用 ai-assistant 接口)。
     */
    private List<ToolSpecification> buildToolSpecs() {
        List<ToolSpecBuilder.ToolInfo> infos = new ArrayList<>();
        for (AssistantTool t : toolRegistry.all()) {
            infos.add(new ToolSpecBuilder.ToolInfo() {
                @Override public String name() { return t.name(); }
                @Override public String description() { return t.description(); }
                @Override public String parametersSchema() { return t.parametersSchema(); }
            });
        }
        return toolSpecBuilder.buildAll(infos);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArgs(String argsJson) {
        if (argsJson == null || argsJson.isBlank()) return new HashMap<>();
        try {
            return JsonUtils.fromJson(argsJson, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("args 解析失败,使用空 map: {}", argsJson, e);
            return new HashMap<>();
        }
    }

    /**
     * 从 DB 加载历史消息并转为 LangChain4j ChatMessage 列表。
     * 不追加 currentUserText(由调用方控制)。
     */
    private List<ChatMessage> buildHistory(Long sessionId) {
        List<AiAssistantMessage> msgs = messageMapper.selectBySessionId(sessionId);
        List<ChatMessage> list = new ArrayList<>();
        for (AiAssistantMessage m : msgs) {
            String role = m.getRole() == null ? "" : m.getRole();
            String content = m.getContent() == null ? "" : m.getContent();
            switch (role) {
                case "user" -> list.add(UserMessage.from(content));
                case "assistant" -> list.add(AiMessage.from(content));
                case "system" -> list.add(SystemMessage.from(content));
                case "tool" -> list.add(ToolExecutionResultMessage.from(
                        m.getToolCallId() == null ? "" : m.getToolCallId(),
                        m.getName() == null ? "" : m.getName(),
                        content));
                default -> { /* 忽略未知角色 */ }
            }
        }
        return list;
    }

    private Long ensureSession(AiAssistantConfig config, AssistantChatRequest req) {
        if (req.getSessionId() != null) {
            AiAssistantSession s = sessionMapper.selectById(req.getSessionId());
            if (s == null) throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
            if (!s.getAssistantId().equals(config.getId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "会话不属于该助手");
            }
            return s.getId();
        }
        Long uid = UserContext.getUserId();
        if (uid == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        AiAssistantSession s = new AiAssistantSession();
        s.setAssistantId(config.getId());
        s.setUserId(uid);
        s.setProjectId(config.getProjectId());
        s.setTitle(req.getTitle() == null ? truncate(config.getName() + " - " + LocalDateTime.now(), 100) : req.getTitle());
        s.setModelId(config.getModelId());
        s.setSystemPrompt(config.getPersona());
        s.setMessageCount(0);
        s.setStatus(1);
        s.setCreateTime(LocalDateTime.now());
        s.setUpdateTime(LocalDateTime.now());
        sessionMapper.insert(s);
        return s.getId();
    }

    private AiAssistantMessage saveMessage(Long sessionId, String role, String content,
                                           String toolCalls, String toolCallId, String name,
                                           Integer inTok, Integer outTok,
                                           Integer costMs, Integer status, String err) {
        AiAssistantMessage m = new AiAssistantMessage();
        m.setSessionId(sessionId);
        m.setRole(role);
        m.setContent(content);
        m.setToolCalls(toolCalls);
        m.setToolCallId(toolCallId);
        m.setName(name);
        m.setInputTokens(inTok);
        m.setOutputTokens(outTok);
        m.setCostMs(costMs);
        m.setStatus(status);
        m.setErrorMsg(err);
        messageMapper.insert(m);
        return m;
    }

    private AiAssistantMessage saveUserMessage(Long sessionId, String content) {
        return saveMessage(sessionId, "user", content, null, null, null, null, null, null, 1, null);
    }

    private AiAssistantMessage saveAssistantMessage(Long sessionId, String content, String toolCalls,
                                                    Integer inTok, Integer outTok, Integer costMs) {
        return saveMessage(sessionId, "assistant", content, toolCalls, null, null, inTok, outTok, costMs, 1, null);
    }

    private ToolResult executeTool(String name, Map<String, Object> args, ToolContext ctx) {
        AssistantTool tool = toolRegistry.get(name);
        if (tool == null) {
            return ToolResult.fail("未找到工具: " + name);
        }
        try {
            return tool.execute(args == null ? Map.of() : args, ctx);
        } catch (Exception e) {
            log.warn("Tool {} execute error", name, e);
            return ToolResult.fail("工具执行异常: " + e.getMessage());
        }
    }

    private void logTool(Long assistantId, Long sessionId, ChatService.ToolCallRequest tc, ToolResult tr, int costMs) {
        try {
            AiAssistantToolLog log = new AiAssistantToolLog();
            log.setAssistantId(assistantId);
            log.setSessionId(sessionId);
            log.setToolName(tc.name());
            log.setArgs(tc.argumentsJson());
            log.setResult(JsonUtils.toJson(tr));
            log.setStatus(tr.isSuccess() ? 1 : 0);
            log.setCostMs(costMs);
            log.setErrorMsg(tr.getError());
            log.setCreateTime(LocalDateTime.now());
            toolLogMapper.insert(log);
        } catch (Exception e) {
            AssistantChatService.log.warn("Failed to log tool execution: {}", tc.name(), e);
        }
    }

    private String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() > n ? s.substring(0, n) + "..." : s;
    }

    public record ChatOutcome(Long sessionId, String content,
                              Integer inputTokens, Integer outputTokens,
                              Integer costMs, List<Map<String, Object>> toolCalls) {}
}
