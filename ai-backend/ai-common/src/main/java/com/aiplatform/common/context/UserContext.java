package com.aiplatform.common.context;

import com.aiplatform.common.constant.SecurityConstants;

import java.io.Serial;
import java.io.Serializable;

/**
 * 当前登录用户上下文(基于 ThreadLocal)。
 */
public class UserContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final ThreadLocal<LoginUser> CONTEXT = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        CONTEXT.set(user);
    }

    public static LoginUser get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static Long getUserId() {
        LoginUser u = CONTEXT.get();
        return u == null ? null : u.getUserId();
    }

    public static String getUsername() {
        LoginUser u = CONTEXT.get();
        return u == null ? SecurityConstants.ANONYMOUS_USER : u.getUsername();
    }

    public static boolean isAuthenticated() {
        return CONTEXT.get() != null;
    }
}
