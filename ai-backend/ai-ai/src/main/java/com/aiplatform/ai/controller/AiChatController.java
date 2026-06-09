package com.aiplatform.ai.controller;

import com.aiplatform.ai.dto.ChatRequest;
import com.aiplatform.ai.dto.ChatResponse;
import com.aiplatform.ai.entity.AiChatMessage;
import com.aiplatform.ai.entity.AiChatSession;
import com.aiplatform.ai.service.AiChatService;
import com.aiplatform.common.api.Result;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService chatService;

    @PostMapping("/session")
    public Result<AiChatSession> createSession(@RequestBody ChatRequest req) {
        return Result.ok(chatService.createSession(req));
    }

    @GetMapping("/sessions")
    public Result<List<AiChatSession>> listSessions() {
        return Result.ok(chatService.listMySessions());
    }

    @GetMapping("/session/{id}")
    public Result<AiChatSession> getSession(@PathVariable Long id) {
        return Result.ok(chatService.getSession(id));
    }

    @GetMapping("/session/{id}/messages")
    public Result<List<AiChatMessage>> listMessages(@PathVariable Long id) {
        return Result.ok(chatService.listMessages(id));
    }

    @DeleteMapping("/session/{id}")
    public Result<Void> deleteSession(@PathVariable Long id) {
        chatService.deleteSession(id);
        return Result.ok();
    }

    @PostMapping("/send")
    public Result<ChatResponse> send(@RequestBody ChatRequest req) {
        if (req.getMessage() == null || req.getMessage().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "消息不能为空");
        }
        return Result.ok(chatService.chat(req));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody ChatRequest req) {
        if (req.getMessage() == null || req.getMessage().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "消息不能为空");
        }
        return chatService.streamChat(req);
    }
}
