package com.aiplatform.system.controller;

import com.aiplatform.common.api.PageResult;
import com.aiplatform.common.api.Result;
import com.aiplatform.framework.security.RequiresPermissions;
import com.aiplatform.system.dto.LogQuery;
import com.aiplatform.system.entity.SysLog;
import com.aiplatform.system.service.SysLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system/log")
@RequiredArgsConstructor
public class SysLogController {

    private final SysLogService logService;

    @GetMapping("/page")
    @RequiresPermissions("system:log:view")
    public Result<PageResult<SysLog>> page(LogQuery q) {
        return Result.ok(logService.page(q));
    }

    @DeleteMapping
    @RequiresPermissions("system:log:delete")
    public Result<Void> delete(@RequestBody List<Long> ids) {
        logService.delete(ids);
        return Result.ok();
    }

    @DeleteMapping("/clear")
    @RequiresPermissions("system:log:delete")
    public Result<Void> clear() {
        logService.clear();
        return Result.ok();
    }
}
