package com.aiplatform.system.controller;

import com.aiplatform.common.api.PageResult;
import com.aiplatform.common.api.Result;
import com.aiplatform.framework.security.RequiresPermissions;
import com.aiplatform.system.dto.UserQuery;
import com.aiplatform.system.dto.UserSaveRequest;
import com.aiplatform.system.dto.UserVO;
import com.aiplatform.system.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    @GetMapping("/page")
    @RequiresPermissions("system:user:view")
    public Result<PageResult<UserVO>> page(UserQuery q) {
        return Result.ok(userService.page(q));
    }

    @GetMapping("/{id}")
    @RequiresPermissions("system:user:view")
    public Result<UserVO> get(@PathVariable Long id) {
        return Result.ok(userService.get(id));
    }

    @PostMapping
    @RequiresPermissions("system:user:add")
    public Result<Long> create(@RequestBody @Valid UserSaveRequest req) {
        return Result.ok(userService.create(req));
    }

    @PutMapping
    @RequiresPermissions("system:user:edit")
    public Result<Void> update(@RequestBody @Valid UserSaveRequest req) {
        userService.update(req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("system:user:delete")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    @DeleteMapping("/batch")
    @RequiresPermissions("system:user:delete")
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        userService.deleteBatch(ids);
        return Result.ok();
    }

    @PostMapping("/{id}/reset-password")
    @RequiresPermissions("system:user:reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userService.resetPassword(id, body == null ? null : body.get("newPassword"));
        return Result.ok();
    }

    @PostMapping("/change-password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body) {
        Long uid = com.aiplatform.common.context.UserContext.getUserId();
        userService.changePassword(uid, body == null ? null : body.get("oldPassword"), body == null ? null : body.get("newPassword"));
        return Result.ok();
    }

    @PostMapping("/{id}/status")
    @RequiresPermissions("system:user:edit")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        userService.updateStatus(id, body == null ? null : body.get("status"));
        return Result.ok();
    }
}
