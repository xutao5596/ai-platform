package com.aiplatform.flow.controller;

import com.aiplatform.common.api.PageResult;
import com.aiplatform.common.api.Result;
import com.aiplatform.flow.dto.CustomNodeSaveRequest;
import com.aiplatform.flow.entity.AiCustomNode;
import com.aiplatform.flow.service.CustomNodeService;
import com.aiplatform.project.annotation.PreProjectRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/flow/custom-nodes")
@RequiredArgsConstructor
public class CustomNodeController {

    private final CustomNodeService customNodeService;

    @GetMapping
    public Result<PageResult<AiCustomNode>> page(@RequestParam(required = false) Long projectId,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(defaultValue = "1") long current,
                                                  @RequestParam(defaultValue = "10") long size) {
        return Result.ok(customNodeService.page(projectId, keyword, current, size));
    }

    @GetMapping("/list")
    public Result<List<AiCustomNode>> list(@RequestParam Long projectId) {
        return Result.ok(customNodeService.listByProject(projectId));
    }

    @GetMapping("/{id}")
    public Result<AiCustomNode> get(@PathVariable Long id) {
        return Result.ok(customNodeService.get(id));
    }

    @PostMapping
    @PreProjectRole("developer")
    public Result<Long> create(@RequestBody CustomNodeSaveRequest req) {
        return Result.ok(customNodeService.create(req));
    }

    @PutMapping
    @PreProjectRole("developer")
    public Result<Void> update(@RequestBody CustomNodeSaveRequest req) {
        customNodeService.update(req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreProjectRole("admin")
    public Result<Void> delete(@PathVariable Long id) {
        customNodeService.delete(id);
        return Result.ok();
    }
}
