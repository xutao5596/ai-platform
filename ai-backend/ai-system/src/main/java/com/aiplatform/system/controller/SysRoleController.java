package com.aiplatform.system.controller;

import com.aiplatform.common.api.PageResult;
import com.aiplatform.common.api.Result;
import com.aiplatform.framework.security.RequiresPermissions;
import com.aiplatform.system.dto.RoleSaveRequest;
import com.aiplatform.system.dto.RoleVO;
import com.aiplatform.system.service.SysRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    @GetMapping("/page")
    @RequiresPermissions("system:role:view")
    public Result<PageResult<RoleVO>> page(@RequestParam(required = false) String keyword,
                                           @RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size) {
        return Result.ok(roleService.page(keyword, current, size));
    }

    @GetMapping("/list")
    @RequiresPermissions("system:role:view")
    public Result<List<RoleVO>> list() {
        return Result.ok(roleService.listAll());
    }

    @GetMapping("/{id}")
    @RequiresPermissions("system:role:view")
    public Result<RoleVO> get(@PathVariable Long id) {
        return Result.ok(roleService.get(id));
    }

    @PostMapping
    @RequiresPermissions("system:role:add")
    public Result<Long> create(@RequestBody @Valid RoleSaveRequest req) {
        return Result.ok(roleService.create(req));
    }

    @PutMapping
    @RequiresPermissions("system:role:edit")
    public Result<Void> update(@RequestBody @Valid RoleSaveRequest req) {
        roleService.update(req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("system:role:delete")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }
}
