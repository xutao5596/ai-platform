package com.aiplatform.ai.controller;

import com.aiplatform.ai.dto.PromptSaveRequest;
import com.aiplatform.ai.dto.PromptVersionRequest;
import com.aiplatform.ai.entity.AiPrompt;
import com.aiplatform.ai.entity.AiPromptVersion;
import com.aiplatform.ai.service.AiPromptService;
import com.aiplatform.common.api.PageResult;
import com.aiplatform.common.api.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/prompt")
@RequiredArgsConstructor
public class AiPromptController {

    private final AiPromptService promptService;

    @GetMapping("/page")
    public Result<PageResult<AiPrompt>> page(@RequestParam(required = false) Long projectId,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "10") long size) {
        return Result.ok(promptService.page(projectId, keyword, current, size));
    }

    @GetMapping("/list")
    public Result<List<AiPrompt>> list(@RequestParam Long projectId) {
        return Result.ok(promptService.listByProject(projectId));
    }

    @GetMapping("/{id}")
    public Result<AiPrompt> get(@PathVariable Long id) {
        return Result.ok(promptService.get(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody @Valid PromptSaveRequest req) {
        return Result.ok(promptService.create(req));
    }

    @PutMapping
    public Result<Void> update(@RequestBody @Valid PromptSaveRequest req) {
        promptService.update(req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        promptService.delete(id);
        return Result.ok();
    }

    @GetMapping("/{id}/versions")
    public Result<List<AiPromptVersion>> versions(@PathVariable Long id) {
        return Result.ok(promptService.listVersions(id));
    }

    @GetMapping("/{id}/active")
    public Result<AiPromptVersion> active(@PathVariable Long id) {
        return Result.ok(promptService.getActiveVersion(id));
    }

    @PostMapping("/version")
    public Result<Long> createVersion(@RequestBody PromptVersionRequest req) {
        if (req.getContent() == null || req.getContent().isBlank()) {
            throw new com.aiplatform.common.exception.BusinessException(
                com.aiplatform.common.exception.ErrorCode.BAD_REQUEST, "content 不能为空");
        }
        return Result.ok(promptService.createVersion(req));
    }

    @PostMapping("/{promptId}/activate/{versionId}")
    public Result<Void> activate(@PathVariable Long promptId, @PathVariable Long versionId) {
        promptService.activateVersion(promptId, versionId);
        return Result.ok();
    }
}
