package com.aiplatform.system.controller;

import com.aiplatform.common.api.Result;
import com.aiplatform.framework.web.WebUtils;
import com.aiplatform.system.dto.LoginRequest;
import com.aiplatform.system.dto.LoginResponse;
import com.aiplatform.system.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody @Valid LoginRequest req) {
        return Result.ok(authService.login(req, WebUtils.getClientIp(currentRequest())));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestBody Map<String, String> body) {
        String token = body == null ? null : body.get("refreshToken");
        return Result.ok(authService.refresh(token));
    }

    @GetMapping("/profile")
    public Result<LoginResponse.UserInfo> profile() {
        return Result.ok(authService.profile());
    }

    @GetMapping("/unauthorized")
    public Result<Void> unauthorized() {
        return Result.unauthorized("未登录或登录已过期");
    }

    private jakarta.servlet.http.HttpServletRequest currentRequest() {
        org.springframework.web.context.request.ServletRequestAttributes attrs =
                (org.springframework.web.context.request.ServletRequestAttributes)
                        org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }
}
