package com.aiplatform.ai.controller;

import com.aiplatform.ai.dto.KnowledgeSaveRequest;
import com.aiplatform.ai.entity.AiKnowledge;
import com.aiplatform.ai.entity.AiKnowledgeDoc;
import com.aiplatform.ai.service.AiKnowledgeDocService;
import com.aiplatform.ai.service.AiKnowledgeService;
import com.aiplatform.common.api.Result;
import com.aiplatform.project.security.ProjectRoleChecker;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/knowledge")
@RequiredArgsConstructor
public class AiKnowledgeController {

    private final AiKnowledgeService knowledgeService;
    private final AiKnowledgeDocService docService;
    private final ProjectRoleChecker projectRoleChecker;

    @GetMapping("/list")
    public Result<List<AiKnowledge>> list(@RequestParam(required = false) Long projectId) {
        if (projectId != null) {
            projectRoleChecker.requireAtLeast(projectId, "viewer");
        }
        return Result.ok(projectId == null ? knowledgeService.listAll() : knowledgeService.listByProject(projectId));
    }

    @GetMapping("/{id}")
    public Result<AiKnowledge> get(@PathVariable Long id) {
        AiKnowledge k = knowledgeService.get(id);
        if (k.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(k.getProjectId(), "viewer");
        }
        return Result.ok(k);
    }

    @PostMapping
    public Result<Long> create(@RequestBody @Valid KnowledgeSaveRequest req) {
        if (req.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(req.getProjectId(), "developer");
        }
        return Result.ok(knowledgeService.create(req));
    }

    @PutMapping
    public Result<Void> update(@RequestBody @Valid KnowledgeSaveRequest req) {
        AiKnowledge old = req.getId() == null ? null : knowledgeService.get(req.getId());
        if (old != null && old.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(old.getProjectId(), "developer");
        }
        knowledgeService.update(req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        AiKnowledge k = knowledgeService.get(id);
        if (k.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(k.getProjectId(), "admin");
        }
        knowledgeService.delete(id);
        return Result.ok();
    }

    @PostMapping(value = "/{id}/doc/upload", consumes = "multipart/form-data")
    public Result<Long> uploadDoc(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        AiKnowledge k = knowledgeService.get(id);
        if (k.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(k.getProjectId(), "developer");
        }
        return Result.ok(docService.upload(id, file));
    }

    @GetMapping("/{id}/doc/list")
    public Result<List<AiKnowledgeDoc>> listDocs(@PathVariable Long id) {
        AiKnowledge k = knowledgeService.get(id);
        if (k.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(k.getProjectId(), "viewer");
        }
        return Result.ok(docService.listByKb(id));
    }

    @DeleteMapping("/{id}/doc/{docId}")
    public Result<Void> deleteDoc(@PathVariable Long id, @PathVariable Long docId) {
        AiKnowledge k = knowledgeService.get(id);
        if (k.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(k.getProjectId(), "admin");
        }
        docService.delete(docId);
        return Result.ok();
    }
}
