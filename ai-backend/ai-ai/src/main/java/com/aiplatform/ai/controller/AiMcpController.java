package com.aiplatform.ai.controller;

import com.aiplatform.ai.entity.AiMcp;
import com.aiplatform.ai.service.AiMcpService;
import com.aiplatform.common.api.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/mcp")
@RequiredArgsConstructor
public class AiMcpController {

    private final AiMcpService mcpService;

    @GetMapping("/list")
    public Result<List<AiMcp>> list() {
        return Result.ok(mcpService.listAll());
    }

    @GetMapping("/{id}")
    public Result<AiMcp> get(@PathVariable Long id) {
        return Result.ok(mcpService.get(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody @Valid AiMcp req) {
        return Result.ok(mcpService.create(req));
    }

    @PutMapping
    public Result<Void> update(@RequestBody @Valid AiMcp req) {
        mcpService.update(req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        mcpService.delete(id);
        return Result.ok();
    }
}
