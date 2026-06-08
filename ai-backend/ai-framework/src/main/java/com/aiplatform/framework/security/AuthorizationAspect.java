package com.aiplatform.framework.security;

import com.aiplatform.common.context.LoginUser;
import com.aiplatform.common.context.UserContext;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 鉴权 AOP:拦截 @RequiresRoles / @RequiresPermissions。
 */
@Slf4j
@Aspect
@Component
public class AuthorizationAspect {

    @Around("@annotation(rr)")
    public Object checkRoles(ProceedingJoinPoint pjp, RequiresRoles rr) throws Throwable {
        check(rr.value(), rr.logical(), true);
        return pjp.proceed();
    }

    @Around("@annotation(rp)")
    public Object checkPermissions(ProceedingJoinPoint pjp, RequiresPermissions rp) throws Throwable {
        check(rp.value(), rp.logical(), false);
        return pjp.proceed();
    }

    private void check(String[] required, Logical logical, boolean isRole) {
        LoginUser user = UserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (user.getAdmin() != null && user.getAdmin()) {
            return;
        }
        Set<String> owned = isRole ? user.getRoles() : user.getPermissions();
        if (owned == null || owned.isEmpty()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        boolean ok;
        if (logical == Logical.AND) {
            ok = true;
            for (String r : required) {
                if (!owned.contains(r)) {
                    ok = false;
                    break;
                }
            }
        } else {
            ok = false;
            for (String r : required) {
                if (owned.contains(r)) {
                    ok = true;
                    break;
                }
            }
        }
        if (!ok) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
