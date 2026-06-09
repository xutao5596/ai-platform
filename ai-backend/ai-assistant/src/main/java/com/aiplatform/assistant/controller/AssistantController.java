package com.aiplatform.assistant.controller;

import com.aiplatform.assistant.dto.AssistantChatRequest;
import com.aiplatform.assistant.dto.AssistantSaveRequest;
import com.aiplatform.ai.entity.AiAssistantConfig;
import com.aiplatform.assistant.service.AssistantChatService;
import com.aiplatform.assistant.service.AssistantService;
import com.aiplatform.common.api.PageResult;
import com.aiplatform.common.api.Result;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;
    private final AssistantChatService chatService;

    @GetMapping("/page")
    public Result<PageResult<AiAssistantConfig>> page(@RequestParam(required = false) Long projectId,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(defaultValue = "1") long current,
                                                      @RequestParam(defaultValue = "10") long size) {
        return Result.ok(assistantService.page(projectId, keyword, current, size));
    }

    @GetMapping("/list")
    public Result<List<AiAssistantConfig>> list(@RequestParam(required = false) Long projectId) {
        return Result.ok(assistantService.listByProject(projectId));
    }

    @GetMapping("/{id}")
    public Result<AiAssistantConfig> get(@PathVariable Long id) {
        return Result.ok(assistantService.get(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody AssistantSaveRequest req) {
        return Result.ok(assistantService.create(req));
    }

    @PutMapping
    public Result<Void> update(@RequestBody AssistantSaveRequest req) {
        assistantService.update(req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        assistantService.delete(id);
        return Result.ok();
    }

    @PostMapping(value = "/{id}/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@PathVariable Long id, @RequestBody AssistantChatRequest req) {
        if (req.getMessage() == null || req.getMessage().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "message 不能为空");
        }
        return chatService.streamChat(id, req);
    }
}
