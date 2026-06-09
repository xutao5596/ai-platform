package com.aiplatform.flow.controller;

import com.aiplatform.common.api.PageResult;
import com.aiplatform.common.api.Result;
import com.aiplatform.flow.dto.FlowSaveRequest;
import com.aiplatform.flow.entity.AiFlow;
import com.aiplatform.flow.service.FlowService;
import com.aiplatform.project.annotation.PreProjectRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/flow")
@RequiredArgsConstructor
public class FlowController {

    private final FlowService flowService;

    @GetMapping("/page")
    public Result<PageResult<AiFlow>> page(@RequestParam(required = false) Long projectId,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(flowService.page(projectId, keyword, current, size));
    }

    @GetMapping("/list")
    public Result<List<AiFlow>> list(@RequestParam Long projectId) {
        return Result.ok(flowService.listByProject(projectId));
    }

    @GetMapping("/{id}")
    public Result<AiFlow> get(@PathVariable Long id) {
        return Result.ok(flowService.getWithPermission(id, "viewer"));
    }

    @PostMapping
    @PreProjectRole("developer")
    public Result<Long> create(@RequestBody FlowSaveRequest req) {
        return Result.ok(flowService.create(req));
    }

    @PutMapping
    @PreProjectRole("developer")
    public Result<Void> update(@RequestBody FlowSaveRequest req) {
        flowService.update(req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreProjectRole("admin")
    public Result<Void> delete(@PathVariable Long id) {
        flowService.delete(id);
        return Result.ok();
    }
}
