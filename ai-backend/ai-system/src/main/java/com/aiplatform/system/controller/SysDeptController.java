package com.aiplatform.system.controller;

import com.aiplatform.common.api.Result;
import com.aiplatform.framework.security.RequiresPermissions;
import com.aiplatform.system.dto.DeptSaveRequest;
import com.aiplatform.system.entity.SysDept;
import com.aiplatform.system.service.SysDeptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system/dept")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService deptService;

    @GetMapping("/list")
    @RequiresPermissions("system:dept:view")
    public Result<List<SysDept>> list() {
        return Result.ok(deptService.listAll());
    }

    @GetMapping("/tree")
    @RequiresPermissions("system:dept:view")
    public Result<List<Map<String, Object>>> tree() {
        return Result.ok(deptService.tree());
    }

    @PostMapping
    @RequiresPermissions("system:dept:add")
    public Result<Long> create(@RequestBody @Valid DeptSaveRequest req) {
        return Result.ok(deptService.create(req));
    }

    @PutMapping
    @RequiresPermissions("system:dept:edit")
    public Result<Void> update(@RequestBody @Valid DeptSaveRequest req) {
        deptService.update(req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("system:dept:delete")
    public Result<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return Result.ok();
    }
}
