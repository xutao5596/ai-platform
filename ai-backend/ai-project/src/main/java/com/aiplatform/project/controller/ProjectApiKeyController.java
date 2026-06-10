package com.aiplatform.project.controller;

import com.aiplatform.common.api.Result;
import com.aiplatform.project.annotation.PreProjectRole;
import com.aiplatform.project.dto.ApiKeySaveRequest;
import com.aiplatform.project.dto.ApiKeyVO;
import com.aiplatform.project.service.ApiKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/project/{projectId}/apikeys")
@RequiredArgsConstructor
public class ProjectApiKeyController {

    private final ApiKeyService apiKeyService;

    @GetMapping
    @PreProjectRole("admin")
    public Result<List<ApiKeyVO>> list(@PathVariable Long projectId) {
        return Result.ok(apiKeyService.list(projectId));
    }

    @PostMapping
    @PreProjectRole("admin")
    public Result<ApiKeyVO> create(@PathVariable Long projectId,
                                   @RequestBody @Valid ApiKeySaveRequest req) {
        return Result.ok(apiKeyService.create(projectId, req));
    }

    @PutMapping("/{id}")
    @PreProjectRole("admin")
    public Result<ApiKeyVO> update(@PathVariable Long projectId,
                                   @PathVariable Long id,
                                   @RequestBody ApiKeySaveRequest req) {
        return Result.ok(apiKeyService.update(projectId, id, req));
    }

    @DeleteMapping("/{id}")
    @PreProjectRole("admin")
    public Result<Void> delete(@PathVariable Long projectId, @PathVariable Long id) {
        apiKeyService.delete(projectId, id);
        return Result.ok();
    }

    @PostMapping("/{id}/reset")
    @PreProjectRole("admin")
    public Result<ApiKeyVO> reset(@PathVariable Long projectId, @PathVariable Long id) {
        return Result.ok(apiKeyService.resetSecret(projectId, id));
    }
}
