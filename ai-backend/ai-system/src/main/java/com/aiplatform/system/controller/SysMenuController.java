package com.aiplatform.system.controller;

import com.aiplatform.common.api.Result;
import com.aiplatform.framework.security.RequiresPermissions;
import com.aiplatform.system.dto.MenuSaveRequest;
import com.aiplatform.system.dto.MenuTreeNode;
import com.aiplatform.system.service.SysMenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService menuService;

    @GetMapping("/tree")
    @RequiresPermissions("system:menu:view")
    public Result<List<MenuTreeNode>> tree() {
        return Result.ok(menuService.tree());
    }

    @GetMapping("/mine")
    public Result<List<MenuTreeNode>> mine() {
        return Result.ok(menuService.listForCurrentUser());
    }

    @GetMapping("/permissions")
    public Result<List<String>> permissions() {
        return Result.ok(menuService.permissionsForCurrentUser());
    }

    @PostMapping
    @RequiresPermissions("system:menu:add")
    public Result<Long> create(@RequestBody @Valid MenuSaveRequest req) {
        return Result.ok(menuService.create(req));
    }

    @PutMapping
    @RequiresPermissions("system:menu:edit")
    public Result<Void> update(@RequestBody @Valid MenuSaveRequest req) {
        menuService.update(req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("system:menu:delete")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.ok();
    }
}
