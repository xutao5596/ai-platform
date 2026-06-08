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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PreProjectRoleAspect {

    private final ProjectRoleChecker checker;

    @Around("@annotation(ppr) || @within(ppr)")
    public Object check(ProceedingJoinPoint pjp, PreProjectRole ppr) throws Throwable {
        Long projectId = resolveProjectId(pjp);
        checker.requireAtLeast(projectId, ppr.value());
        return pjp.proceed();
    }

    private Long resolveProjectId(ProceedingJoinPoint pjp) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        Annotation[][] pas = method.getParameterAnnotations();
        Object[] args = pjp.getArgs();
        for (int i = 0; i < args.length; i++) {
            for (Annotation a : pas[i]) {
                if (a instanceof PathVariable pv && "projectId".equals(pv.value())) {
                    return toLong(args[i]);
                }
            }
        }
        if (args.length == 1 && args[0] != null) {
            try {
                var f = args[0].getClass().getDeclaredField("projectId");
                f.setAccessible(true);
                return toLong(f.get(args[0]));
            } catch (Exception ignored) {
            }
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "无法解析项目 id");
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
