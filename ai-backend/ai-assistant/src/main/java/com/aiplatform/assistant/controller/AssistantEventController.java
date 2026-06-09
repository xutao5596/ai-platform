package com.aiplatform.assistant.controller;

import com.aiplatform.assistant.dto.EventSubRequest;
import com.aiplatform.ai.entity.AiAssistantEventSub;
import com.aiplatform.assistant.service.AssistantEventService;
import com.aiplatform.common.api.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assistant/{id}/events")
@RequiredArgsConstructor
public class AssistantEventController {

    private final AssistantEventService eventService;

    @GetMapping
    public Result<List<AiAssistantEventSub>> list(@PathVariable("id") Long assistantId) {
        return Result.ok(eventService.listByAssistant(assistantId));
    }

    @PostMapping
    public Result<Long> subscribe(@PathVariable("id") Long assistantId, @RequestBody EventSubRequest req) {
        return Result.ok(eventService.subscribe(assistantId, req));
    }

    @DeleteMapping("/{eventId}")
    public Result<Void> unsubscribe(@PathVariable("id") Long assistantId, @PathVariable("eventId") Long eventId) {
        eventService.unsubscribe(assistantId, eventId);
        return Result.ok();
    }
}
