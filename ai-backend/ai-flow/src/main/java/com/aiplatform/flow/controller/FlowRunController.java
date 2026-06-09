package com.aiplatform.flow.controller;

import com.aiplatform.common.api.PageResult;
import com.aiplatform.common.api.Result;
import com.aiplatform.flow.dto.FlowRunRequest;
import com.aiplatform.flow.entity.AiFlowRun;
import com.aiplatform.flow.entity.AiFlowRunStep;
import com.aiplatform.flow.service.FlowRunService;
import com.aiplatform.project.annotation.PreProjectRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/flow")
@RequiredArgsConstructor
public class FlowRunController {

    private final FlowRunService runService;

    @PostMapping("/{id}/run")
    @PreProjectRole("viewer")
    public Result<AiFlowRun> run(@PathVariable Long id, @RequestBody(required = false) FlowRunRequest req) {
        return Result.ok(runService.run(id, req));
    }

    @GetMapping("/{id}/runs")
    public Result<PageResult<AiFlowRun>> runs(@PathVariable Long id,
                                                @RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "10") long size) {
        return Result.ok(runService.page(id, current, size));
    }

    @GetMapping("/run/{runId}")
    public Result<Map<String, Object>> runDetail(@PathVariable Long runId) {
        return Result.ok(runService.getSummary(runId));
    }

    @GetMapping("/run/{runId}/steps")
    public Result<List<AiFlowRunStep>> steps(@PathVariable Long runId) {
        return Result.ok(runService.steps(runId));
    }

    @PostMapping("/{id}/trigger/test")
    @PreProjectRole("developer")
    public Result<AiFlowRun> testTrigger(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String triggerType = (String) body.get("triggerType");
        @SuppressWarnings("unchecked")
        Map<String, Object> input = (Map<String, Object>) body.get("input");
        if (triggerType == null || triggerType.isBlank()) triggerType = "manual";
        return Result.ok(runService.testTrigger(id, triggerType, input));
    }
}
