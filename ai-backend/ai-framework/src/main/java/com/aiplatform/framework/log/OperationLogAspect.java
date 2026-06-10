package com.aiplatform.framework.log;

import com.aiplatform.common.context.LoginUser;
import com.aiplatform.common.context.UserContext;
import com.aiplatform.framework.web.WebUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 审计日志切面:拦截所有 {@link RestController} 的公开方法,写入 sys_log。
 * <p>
 * 行为契约见 docs/api/sprint5-contracts.md §3。
 * - 自动按 Controller 类名/方法名生成 module/action
 * - 排除白名单(登录/验证码/refresh/webhook 接收/actuator/swagger)
 * - 异步写库,失败不抛(切面吞掉所有异常)
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    private static final Set<String> EXCLUDED_PATH_PREFIXES = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/captcha",
            "/api/v1/auth/refresh",
            "/api/v1/webhook/receive",
            "/api/v1/ext/"
    );

    private static final Set<String> EXCLUDED_PATH_EXACT = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/captcha",
            "/api/v1/auth/refresh"
    );

    private final ObjectProvider<OperationLogRecorder> recorderProvider;
    private final ThreadPoolTaskExecutor operationLogExecutor;
    private final ObjectMapper objectMapper;

    public OperationLogAspect(ObjectProvider<OperationLogRecorder> recorderProvider,
                              ThreadPoolTaskExecutor operationLogExecutor,
                              ObjectMapper objectMapper) {
        this.recorderProvider = recorderProvider;
        this.operationLogExecutor = operationLogExecutor;
        this.objectMapper = objectMapper;
    }

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)"
            + " && execution(public * *(..))")
    public void restControllerMethod() {
    }

    @Around("restControllerMethod()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        ServletRequestAttributes attrs = currentAttrs();
        HttpServletRequest request = attrs == null ? null : attrs.getRequest();

        if (shouldSkip(request)) {
            return pjp.proceed();
        }

        Object result = null;
        Throwable thrown = null;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable t) {
            thrown = t;
            throw t;
        } finally {
            try {
                OperationLogRecorder.OperationLogSnapshot snap = buildSnapshot(pjp, request, result, thrown, System.currentTimeMillis() - start);
                submitAsync(snap);
            } catch (Exception ex) {
                log.warn("operation log build/submit failed: {}", ex.getMessage());
            }
        }
    }

    private boolean shouldSkip(HttpServletRequest req) {
        if (req == null) return true;
        String uri = req.getRequestURI();
        if (uri == null) return true;
        if (EXCLUDED_PATH_EXACT.contains(uri)) return true;
        for (String p : EXCLUDED_PATH_PREFIXES) {
            if (uri.startsWith(p)) return true;
        }
        if (uri.startsWith("/actuator")) return true;
        if (uri.startsWith("/v3/api-docs")) return true;
        if (uri.startsWith("/swagger-ui")) return true;
        return false;
    }

    private OperationLogRecorder.OperationLogSnapshot buildSnapshot(ProceedingJoinPoint pjp,
                                                                    HttpServletRequest request,
                                                                    Object result,
                                                                    Throwable thrown,
                                                                    long costMs) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        String controllerName = pjp.getTarget().getClass().getSimpleName();
        String module = toModuleName(controllerName);
        String action = method.getName();

        OperationLogRecorder.OperationLogSnapshot r = new OperationLogRecorder.OperationLogSnapshot();
        r.setModule(module);
        r.setAction(action);
        r.setMethod(controllerName + "." + method.getName());
        r.setRequestUrl(request == null ? null : buildRequestUrl(request));
        r.setRequestMethod(request == null ? null : request.getMethod());
        r.setRequestParams(extractRequestParams(pjp, request));
        r.setResponseData(extractResponseData(result));
        r.setUserAgent(request == null ? null : truncate(request.getHeader("User-Agent"), 512));
        r.setIp(request == null ? "unknown" : WebUtils.getClientIp(request));
        r.setCostMs(costMs);
        r.setCreateTime(LocalDateTime.now());

        LoginUser user = UserContext.get();
        if (user != null) {
            r.setUserId(user.getUserId());
            r.setUsername(user.getUsername());
        } else {
            r.setUsername("anonymous");
        }

        if (thrown != null) {
            r.setStatus(0);
            String msg = thrown.getMessage() == null ? thrown.getClass().getSimpleName() : thrown.getMessage();
            r.setErrorMsg(truncate(msg, 1024));
        } else {
            r.setStatus(1);
        }
        return r;
    }

    private String toModuleName(String controllerName) {
        String name = controllerName;
        if (name.endsWith("Controller")) {
            name = name.substring(0, name.length() - "Controller".length());
        }
        if (name.isEmpty()) return "unknown";
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private String buildRequestUrl(HttpServletRequest req) {
        String q = req.getQueryString();
        return q == null ? req.getRequestURI() : req.getRequestURI() + "?" + q;
    }

    private String extractRequestParams(ProceedingJoinPoint pjp, HttpServletRequest request) {
        if (request == null) return null;
        String method = request.getMethod();
        boolean isBodyMethod = "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
        if (isBodyMethod) {
            byte[] cached = null;
            if (request instanceof ContentCachingRequestWrapper wrap) {
                cached = wrap.getContentAsByteArray();
            }
            if (cached != null && cached.length > 0) {
                String body = new String(cached, StandardCharsets.UTF_8);
                return truncate(maskSensitive(body), RequestResponseCapture.maxCaptureBytes());
            }
        }
        Object[] args = pjp.getArgs();
        if (args == null || args.length == 0) return null;
        String serialized = safeSerialize(args);
        return truncate(maskSensitive(serialized), RequestResponseCapture.maxCaptureBytes());
    }

    private String extractResponseData(Object result) {
        if (result == null) return null;
        String s = safeSerialize(result);
        return truncate(s, RequestResponseCapture.maxCaptureBytes());
    }

    private String safeSerialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }

    private String maskSensitive(String raw) {
        if (raw == null || raw.isEmpty()) return raw;
        return raw
                .replaceAll("(?i)(\"password\"\\s*:\\s*\")[^\"]*(\")", "$1******$2")
                .replaceAll("(?i)(\"oldPassword\"\\s*:\\s*\")[^\"]*(\")", "$1******$2")
                .replaceAll("(?i)(\"newPassword\"\\s*:\\s*\")[^\"]*(\")", "$1******$2")
                .replaceAll("(?i)(\"refreshToken\"\\s*:\\s*\")[^\"]*(\")", "$1******$2")
                .replaceAll("(?i)(\"token\"\\s*:\\s*\")[^\"]*(\")", "$1******$2");
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...(truncated)";
    }

    private ServletRequestAttributes currentAttrs() {
        return (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    }

    private void submitAsync(OperationLogRecorder.OperationLogSnapshot snap) {
        OperationLogRecorder recorder = recorderProvider.getIfAvailable();
        if (recorder == null) {
            return;
        }
        try {
            operationLogExecutor.execute(() -> {
                try {
                    recorder.record(snap);
                } catch (Exception e) {
                    log.warn("operation log persist failed: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("operation log submit failed: {}", e.getMessage());
        }
    }
}
