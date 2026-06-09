package com.aiplatform.ai.controller;

import com.aiplatform.ai.dto.ModelSaveRequest;
import com.aiplatform.ai.entity.AiModel;
import com.aiplatform.ai.service.AiModelService;
import com.aiplatform.common.api.PageResult;
import com.aiplatform.common.api.Result;
import com.aiplatform.framework.security.RequiresPermissions;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai/model")
@RequiredArgsConstructor
public class AiModelController {

    private final AiModelService modelService;

    @GetMapping("/page")
    @RequiresPermissions("ai:model:view")
    public Result<PageResult<AiModel>> page(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String provider,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(modelService.page(keyword, provider, status, current, size));
    }

    @GetMapping("/list")
    public Result<List<AiModel>> list() {
        return Result.ok(modelService.listEnabled());
    }

    @GetMapping("/embedding")
    public Result<List<AiModel>> embedding() {
        return Result.ok(modelService.listEmbeddingModels());
    }

    @GetMapping("/{id}")
    @RequiresPermissions("ai:model:view")
    public Result<AiModel> get(@PathVariable Long id) {
        return Result.ok(modelService.get(id));
    }

    @PostMapping
    @RequiresPermissions("ai:model:add")
    public Result<Long> create(@RequestBody @Valid ModelSaveRequest req) {
        return Result.ok(modelService.create(req));
    }

    @PutMapping
    @RequiresPermissions("ai:model:edit")
    public Result<Void> update(@RequestBody @Valid ModelSaveRequest req) {
        modelService.update(req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("ai:model:delete")
    public Result<Void> delete(@PathVariable Long id) {
        modelService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/test")
    @RequiresPermissions("ai:model:view")
    public Result<Map<String, Object>> test(@PathVariable Long id) {
        boolean ok = modelService.test(id);
        return Result.ok(Map.of("success", ok));
    }
}
