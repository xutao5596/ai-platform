package com.aiplatform.assistant.service;

import com.aiplatform.ai.llm.ChatService;
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
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 助手对话服务:带工具调用的循环,简化版 PoC。
 * Sprint 3 简化:用 prompt 引导 LLM 输出 JSON 工具调用指令(而非 LangChain4j Function Calling)。
 * 生产应替换为 Function Calling。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantChatService {

    /** 最大工具调用轮次(防死循环) */
    private static final int MAX_TOOL_ROUNDS = 5;
    /** 每次 SSE content chunk 字符数 */
    private static final int SSE_CHUNK_SIZE = 8;
    /** chunk 间隔(毫秒) */
    private static final int SSE_CHUNK_DELAY_MS = 25;

    private final AssistantService assistantService;
    private final AiAssistantSessionMapper sessionMapper;
    private final AiAssistantMessageMapper messageMapper;
    private final AiAssistantToolLogMapper toolLogMapper;
    private final AiAssistantConfigMapper configMapper;
    private final ToolRegistry toolRegistry;
    private final ChatService chatService;

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
        // 保存 user 消息
        AiAssistantMessage userMsg = saveUserMessage(sessionId, req.getMessage());

        long t0 = System.currentTimeMillis();
        List<Map<String, Object>> history = loadHistory(sessionId);
        AtomicInteger inputTokensTotal = new AtomicInteger();
        AtomicInteger outputTokensTotal = new AtomicInteger();

        StringBuilder finalContent = new StringBuilder();
        List<Map<String, Object>> toolCallsRecord = new ArrayList<>();
        String currentMessage = req.getMessage();
        boolean toolsEnabled = req.getToolsEnabled() == null ? true : req.getToolsEnabled();

        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            String prompt = buildPrompt(config, history, currentMessage, toolsEnabled);
            ChatService.ChatResult r = chatService.chat(
                    config.getModelId(),
                    null,
                    prompt,
                    config.getTemperature() == null ? 0.7 : config.getTemperature().doubleValue(),
                    4096);
            if (r.inputTokens() != null) inputTokensTotal.addAndGet(r.inputTokens());
            if (r.outputTokens() != null) outputTokensTotal.addAndGet(r.outputTokens());

            String content = r.content() == null ? "" : r.content();
            // 尝试解析工具调用
            ParsedToolCall tc = tryParseToolCall(content);
            if (tc != null && toolsEnabled) {
                // 把这轮 AI 原始输出保存(content 包含 tool_call JSON)
                Map<String, Object> rec = new HashMap<>();
                rec.put("name", tc.name);
                rec.put("args", tc.args);
                rec.put("callId", tc.callId);
                toolCallsRecord.add(rec);
                history.add(historyOf("assistant", content, tc));
                // 执行工具
                long t1 = System.currentTimeMillis();
                ToolContext ctx = new ToolContext(assistantId, sessionId,
                        UserContext.getUserId(), config.getProjectId());
                ToolResult tr = executeTool(tc.name, tc.args, ctx);
                int cost = (int) (System.currentTimeMillis() - t1);
                logTool(assistantId, sessionId, tc, tr, cost);
                history.add(historyOf("tool", tr.getOutput() == null ? "" : tr.getOutput(), tc.callId, tc.name));
                // 继续下一轮
                continue;
            }
            // 普通文本输出 → 完成
            finalContent.append(content);
            // 保存 assistant 消息
            saveAssistantMessage(sessionId, content,
                    toolCallsRecord.isEmpty() ? null : JsonUtils.toJson(toolCallsRecord),
                    inputTokensTotal.get(), outputTokensTotal.get(),
                    (int) (System.currentTimeMillis() - t0));
            // 更新 session
            session.setMessageCount((session.getMessageCount() == null ? 0 : session.getMessageCount()) + 2);
            session.setUpdateTime(LocalDateTime.now());
            sessionMapper.updateById(session);
            return new ChatOutcome(sessionId, finalContent.toString(),
                    inputTokensTotal.get(), outputTokensTotal.get(),
                    (int) (System.currentTimeMillis() - t0), toolCallsRecord);
        }
        // 超过最大轮次 → 兜底
        log.warn("Assistant chat exceeded max tool rounds: assistantId={}, sessionId={}", assistantId, sessionId);
        return new ChatOutcome(sessionId, finalContent.toString() + "\n[已达最大工具调用轮次]",
                inputTokensTotal.get(), outputTokensTotal.get(),
                (int) (System.currentTimeMillis() - t0), toolCallsRecord);
    }

    /**
     * SSE 流式对话。
     * 事件:
     *  - content    → {"chunk":"...","sessionId":1}
     *  - tool_call  → {"name":"...","args":{...},"callId":"tc-001"}
     *  - tool_result→ {"callId":"tc-001","result":"..."}
     *  - done       → {"sessionId":1,"messageId":42,"inputTokens":120,"outputTokens":85}
     */
    public SseEmitter streamChat(Long assistantId, AssistantChatRequest req) {
        SseEmitter emitter = new SseEmitter(120_000L);
        // 捕获当前线程的 UserContext(SSE 在新线程,需要传递)
        com.aiplatform.common.context.LoginUser loginUser = com.aiplatform.common.context.UserContext.get();
        Thread t = new Thread(() -> {
            try {
                if (loginUser != null) {
                    com.aiplatform.common.context.UserContext.set(loginUser);
                }
                AiAssistantConfig config = assistantService.get(assistantId);
                Long sessionId = ensureSession(config, req);
                AiAssistantSession session = sessionMapper.selectById(sessionId);

                // 保存 user 消息
                AiAssistantMessage userMsg = saveUserMessage(sessionId, req.getMessage());

                // 推送 sessionId
                emitter.send(SseEmitter.event().name("meta").data(Map.of("sessionId", sessionId, "messageId", userMsg.getId())));

                long t0 = System.currentTimeMillis();
                List<Map<String, Object>> history = loadHistory(sessionId);
                AtomicInteger inputTokensTotal = new AtomicInteger();
                AtomicInteger outputTokensTotal = new AtomicInteger();

                List<Map<String, Object>> toolCallsRecord = new ArrayList<>();
                StringBuilder finalContent = new StringBuilder();
                String currentMessage = req.getMessage();
                boolean toolsEnabled = req.getToolsEnabled() == null ? true : req.getToolsEnabled();

                Long finalMessageId = null;
                for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
                    String prompt = buildPrompt(config, history, currentMessage, toolsEnabled);
                    ChatService.ChatResult r = chatService.chat(
                            config.getModelId(),
                            null, prompt,
                            config.getTemperature() == null ? 0.7 : config.getTemperature().doubleValue(),
                            4096);
                    if (r.inputTokens() != null) inputTokensTotal.addAndGet(r.inputTokens());
                    if (r.outputTokens() != null) outputTokensTotal.addAndGet(r.outputTokens());

                    String content = r.content() == null ? "" : r.content();
                    ParsedToolCall tc = tryParseToolCall(content);
                    if (tc != null && toolsEnabled) {
                        Map<String, Object> rec = new HashMap<>();
                        rec.put("name", tc.name);
                        rec.put("args", tc.args);
                        rec.put("callId", tc.callId);
                        toolCallsRecord.add(rec);
                        history.add(historyOf("assistant", content, tc));
                        // 推送 tool_call 事件
                        Map<String, Object> tcEvt = new HashMap<>();
                        tcEvt.put("name", tc.name);
                        tcEvt.put("args", tc.args);
                        tcEvt.put("callId", tc.callId);
                        emitter.send(SseEmitter.event().name("tool_call").data(tcEvt));

                        long t1 = System.currentTimeMillis();
                        ToolContext ctx = new ToolContext(assistantId, sessionId,
                                UserContext.getUserId(), config.getProjectId());
                        ToolResult tr = executeTool(tc.name, tc.args, ctx);
                        int cost = (int) (System.currentTimeMillis() - t1);
                        logTool(assistantId, sessionId, tc, tr, cost);

                        // 推送 tool_result 事件
                        Map<String, Object> trEvt = new HashMap<>();
                        trEvt.put("callId", tc.callId);
                        trEvt.put("success", tr.isSuccess());
                        trEvt.put("output", tr.getOutput() == null ? "" : tr.getOutput());
                        if (tr.getError() != null) trEvt.put("error", tr.getError());
                        emitter.send(SseEmitter.event().name("tool_result").data(trEvt));

                        history.add(historyOf("tool", tr.getOutput() == null ? "" : tr.getOutput(), tc.callId, tc.name));
                        continue;
                    }
                    // 普通文本 → 分块推送
                    finalContent.append(content);
                    for (int i = 0; i < content.length(); i += SSE_CHUNK_SIZE) {
                        int end = Math.min(content.length(), i + SSE_CHUNK_SIZE);
                        String chunk = content.substring(i, end);
                        Map<String, Object> evt = new HashMap<>();
                        evt.put("chunk", chunk);
                        evt.put("sessionId", sessionId);
                        emitter.send(SseEmitter.event().name("content").data(evt));
                        Thread.sleep(SSE_CHUNK_DELAY_MS);
                    }
                    // 保存 assistant 消息
                    AiAssistantMessage aiMsg = saveAssistantMessage(sessionId, content,
                            toolCallsRecord.isEmpty() ? null : JsonUtils.toJson(toolCallsRecord),
                            inputTokensTotal.get(), outputTokensTotal.get(),
                            (int) (System.currentTimeMillis() - t0));
                    finalMessageId = aiMsg.getId();
                    // 更新 session
                    session.setMessageCount((session.getMessageCount() == null ? 0 : session.getMessageCount()) + 2);
                    session.setUpdateTime(LocalDateTime.now());
                    sessionMapper.updateById(session);
                    // 推送 done
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
                // 超过最大轮次
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
            } catch (IOException | InterruptedException ex) {
                emitter.completeWithError(ex);
            } catch (Exception ex) {
                log.error("Assistant stream error", ex);
                try {
                    emitter.send(SseEmitter.event().name("error").data(Map.of("message", ex.getMessage())));
                } catch (IOException ignored) {
                }
                emitter.completeWithError(ex);
            } finally {
                com.aiplatform.common.context.UserContext.clear();
            }
        }, "assistant-sse-" + assistantId + "-" + System.currentTimeMillis());
        t.start();
        return emitter;
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

    private List<Map<String, Object>> loadHistory(Long sessionId) {
        List<AiAssistantMessage> msgs = messageMapper.selectBySessionId(sessionId);
        List<Map<String, Object>> list = new ArrayList<>();
        for (AiAssistantMessage m : msgs) {
            // 跳过最近一条(就是当前 user message)
            Map<String, Object> h = new HashMap<>();
            h.put("role", m.getRole());
            h.put("content", m.getContent() == null ? "" : m.getContent());
            if (m.getToolCalls() != null) h.put("tool_calls", m.getToolCalls());
            if (m.getToolCallId() != null) h.put("tool_call_id", m.getToolCallId());
            if (m.getName() != null) h.put("name", m.getName());
            list.add(h);
        }
        return list;
    }

    private String buildPrompt(AiAssistantConfig config, List<Map<String, Object>> history, String userMsg, boolean toolsEnabled) {
        StringBuilder sb = new StringBuilder();
        // system prompt
        if (config.getPersona() != null && !config.getPersona().isBlank()) {
            sb.append("[系统提示] ").append(config.getPersona()).append("\n\n");
        }
        // 工具说明
        if (toolsEnabled) {
            sb.append("[可用工具]\n");
            int i = 0;
            for (AssistantTool t : toolRegistry.all()) {
                sb.append(++i).append(". ")
                        .append(t.name()).append(" — ").append(t.displayName()).append(": ")
                        .append(t.description()).append("\n   入参: ").append(t.parametersSchema()).append("\n");
            }
            sb.append("\n如果需要调用工具,请仅输出一行 JSON(不要其他文字):")
                    .append("{\"tool\":\"<name>\",\"args\":{...}}\n\n");
        }
        // history
        if (!history.isEmpty()) {
            sb.append("[历史消息]\n");
            for (Map<String, Object> h : history) {
                sb.append("- ").append(h.get("role")).append(": ");
                if (h.containsKey("name")) sb.append("[name=").append(h.get("name")).append("] ");
                sb.append(truncate(String.valueOf(h.get("content")), 500)).append("\n");
            }
            sb.append("\n");
        }
        sb.append("[用户] ").append(userMsg);
        return sb.toString();
    }

    /**
     * 解析 LLM 输出中的工具调用:支持两种格式
     *  1) 整段 JSON  {"tool":"calculator","args":{"expression":"1+1"}}
     *  2) JSON 嵌入在 ```json ... ``` 代码块中
     */
    static ParsedToolCall tryParseToolCall(String content) {
        if (content == null) return null;
        String trimmed = content.trim();
        // 提取代码块
        if (trimmed.startsWith("```")) {
            int firstNl = trimmed.indexOf('\n');
            if (firstNl > 0) {
                int lastBack = trimmed.lastIndexOf("```");
                if (lastBack > firstNl) {
                    trimmed = trimmed.substring(firstNl + 1, lastBack).trim();
                }
            }
        }
        // 必须以 { 开头
        if (!trimmed.startsWith("{")) return null;
        // 找到匹配的 }
        int depth = 0;
        int end = -1;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    end = i;
                    break;
                }
            }
        }
        if (end < 0) return null;
        String json = trimmed.substring(0, end + 1);
        try {
            Map<String, Object> map = JsonUtils.fromJson(json, new TypeReference<Map<String, Object>>() {});
            if (map == null) return null;
            Object tool = map.get("tool");
            if (tool == null) return null;
            String name = String.valueOf(tool);
            Object argsObj = map.get("args");
            Map<String, Object> args = new HashMap<>();
            if (argsObj instanceof Map<?, ?> m) {
                m.forEach((k, v) -> args.put(String.valueOf(k), v));
            }
            String callId = "tc-" + UUID.randomUUID().toString().substring(0, 8);
            return new ParsedToolCall(callId, name, args, content);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> historyOf(String role, String content, ParsedToolCall tc) {
        Map<String, Object> h = new HashMap<>();
        h.put("role", role);
        h.put("content", content);
        h.put("tool_calls", JsonUtils.toJson(List.of(Map.of(
                "id", tc.callId, "name", tc.name, "args", tc.args
        ))));
        return h;
    }

    private Map<String, Object> historyOf(String role, String content, String toolCallId, String name) {
        Map<String, Object> h = new HashMap<>();
        h.put("role", role);
        h.put("content", content);
        h.put("tool_call_id", toolCallId);
        h.put("name", name);
        return h;
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

    private void logTool(Long assistantId, Long sessionId, ParsedToolCall tc, ToolResult tr, int costMs) {
        try {
            AiAssistantToolLog log = new AiAssistantToolLog();
            log.setAssistantId(assistantId);
            log.setSessionId(sessionId);
            log.setToolName(tc.name);
            log.setArgs(JsonUtils.toJson(tc.args));
            log.setResult(JsonUtils.toJson(tr));
            log.setStatus(tr.isSuccess() ? 1 : 0);
            log.setCostMs(costMs);
            log.setErrorMsg(tr.getError());
            log.setCreateTime(LocalDateTime.now());
            toolLogMapper.insert(log);
        } catch (Exception e) {
            AssistantChatService.log.warn("Failed to log tool execution: {}", tc.name, e);
        }
    }

    private String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() > n ? s.substring(0, n) + "..." : s;
    }

    public record ChatOutcome(Long sessionId, String content,
                              Integer inputTokens, Integer outputTokens,
                              Integer costMs, List<Map<String, Object>> toolCalls) {}

    static record ParsedToolCall(String callId, String name, Map<String, Object> args, String rawContent) {}
}
