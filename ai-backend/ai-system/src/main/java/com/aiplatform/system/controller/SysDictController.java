package com.aiplatform.system.controller;

import com.aiplatform.common.api.PageResult;
import com.aiplatform.common.api.Result;
import com.aiplatform.framework.security.RequiresPermissions;
import com.aiplatform.system.dto.DictItemSaveRequest;
import com.aiplatform.system.dto.DictSaveRequest;
import com.aiplatform.system.entity.SysDict;
import com.aiplatform.system.entity.SysDictItem;
import com.aiplatform.system.service.SysDictService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system/dict")
@RequiredArgsConstructor
public class SysDictController {

    private final SysDictService dictService;

    @GetMapping("/page")
    @RequiresPermissions("system:dict:view")
    public Result<PageResult<SysDict>> page(@RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(dictService.page(keyword, current, size));
    }

    @GetMapping("/list")
    public Result<List<SysDict>> list() {
        return Result.ok(dictService.listAll());
    }

    @GetMapping("/items")
    public Result<List<SysDictItem>> items(@RequestParam String typeCode) {
        return Result.ok(dictService.itemsByType(typeCode));
    }

    @PostMapping
    @RequiresPermissions("system:dict:add")
    public Result<Long> createDict(@RequestBody @Valid DictSaveRequest req) {
        return Result.ok(dictService.createDict(req));
    }

    @PutMapping
    @RequiresPermissions("system:dict:edit")
    public Result<Void> updateDict(@RequestBody @Valid DictSaveRequest req) {
        dictService.updateDict(req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("system:dict:delete")
    public Result<Void> deleteDict(@PathVariable Long id) {
        dictService.deleteDict(id);
        return Result.ok();
    }

    @PostMapping("/item")
    @RequiresPermissions("system:dict:add")
    public Result<Long> createItem(@RequestBody @Valid DictItemSaveRequest req) {
        return Result.ok(dictService.createItem(req));
    }

    @PutMapping("/item")
    @RequiresPermissions("system:dict:edit")
    public Result<Void> updateItem(@RequestBody @Valid DictItemSaveRequest req) {
        dictService.updateItem(req);
        return Result.ok();
    }

    @DeleteMapping("/item/{id}")
    @RequiresPermissions("system:dict:delete")
    public Result<Void> deleteItem(@PathVariable Long id) {
        dictService.deleteItem(id);
        return Result.ok();
    }
}
