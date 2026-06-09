package com.aiplatform.flow.controller;

import com.aiplatform.common.api.Result;
import com.aiplatform.flow.dto.FlowVersionRequest;
import com.aiplatform.flow.entity.AiFlowVersion;
import com.aiplatform.flow.service.FlowVersionService;
import com.aiplatform.project.annotation.PreProjectRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/flow/{id}/versions")
@RequiredArgsConstructor
public class FlowVersionController {

    private final FlowVersionService versionService;

    @GetMapping
    public Result<List<AiFlowVersion>> list(@PathVariable Long id) {
        return Result.ok(versionService.list(id));
    }

    @PostMapping
    @PreProjectRole("developer")
    public Result<Long> create(@PathVariable Long id, @RequestBody FlowVersionRequest req) {
        return Result.ok(versionService.createVersion(id, req));
    }

    @PostMapping("/publish/{versionId}")
    @PreProjectRole("admin")
    public Result<Void> publish(@PathVariable Long id, @PathVariable Long versionId) {
        versionService.publish(id, versionId);
        return Result.ok();
    }
}
