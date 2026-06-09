package com.aiplatform.project.security;

import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import com.aiplatform.project.annotation.PreProjectRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PreProjectRoleAspect {

    private final ProjectRoleChecker checker;

    @Around("@annotation(ppr)")
    public Object checkAnnotation(ProceedingJoinPoint pjp, PreProjectRole ppr) throws Throwable {
        return doCheck(pjp, ppr);
    }

    @Around("@within(ppr)")
    public Object checkWithin(ProceedingJoinPoint pjp, PreProjectRole ppr) throws Throwable {
        return doCheck(pjp, ppr);
    }

    private Object doCheck(ProceedingJoinPoint pjp, PreProjectRole ppr) throws Throwable {
        if (ppr == null) {
            // @within 形式下若类上无注解,ppr 可能为 null,直接放行(由调用方 @annotation 保证)
            return pjp.proceed();
        }
        Long projectId = resolveProjectId(pjp);
        checker.requireAtLeast(projectId, ppr.value());
        return pjp.proceed();
    }

    private Long resolveProjectId(ProceedingJoinPoint pjp) {
        // 1. 优先从 URI 模板变量取值(@PathVariable 编译擦除不影响 URI 模板)
        Long fromUri = extractFromUri("projectId");
        if (fromUri != null) return fromUri;
        // 兼容路径是 {id} 的场景
        Long fromId = extractFromUri("id");
        if (fromId != null) return fromId;

        // 2. 回退:方法参数上的 @PathVariable 显式 value
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        Annotation[][] pas = method.getParameterAnnotations();
        Object[] args = pjp.getArgs();
        for (int i = 0; i < args.length; i++) {
            for (Annotation a : pas[i]) {
                if (a instanceof PathVariable pv) {
                    String name = pv.value();
                    if ("projectId".equals(name) || "id".equals(name)) {
                        return toLong(args[i]);
                    }
                }
            }
        }
        // 3. 回退:参数对象字段(直接 projectId 字段)
        if (args.length >= 1 && args[0] != null) {
            try {
                var f = args[0].getClass().getDeclaredField("projectId");
                f.setAccessible(true);
                Long v = toLong(f.get(args[0]));
                if (v != null) return v;
            } catch (NoSuchFieldException ignored) {
            } catch (Exception e) {
                log.debug("Failed to read projectId field: {}", e.getMessage());
            }
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "无法解析项目 id");
    }

    @SuppressWarnings("unchecked")
    private Long extractFromUri(String varName) {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest request = attrs.getRequest();
            Map<String, String> uriTemplateVariables =
                    (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if (uriTemplateVariables == null) return null;
            String v = uriTemplateVariables.get(varName);
            return v == null ? null : toLong(v);
        } catch (Exception e) {
            log.debug("Failed to extract {} from URI: {}", varName, e.getMessage());
            return null;
        }
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Long l) return l;
        if (o instanceof Number n) return n.longValue();
        if (o instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
