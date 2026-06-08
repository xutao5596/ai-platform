package com.aiplatform.system.service;

import cn.hutool.crypto.SecureUtil;
import com.aiplatform.common.constant.CommonConstants;
import com.aiplatform.system.entity.SysUser;

public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String hash(String raw) {
        return SecureUtil.sha256(CommonConstants.YES + raw + CommonConstants.NO);
    }

    public static boolean matches(String raw, String hashed) {
        if (raw == null || hashed == null) {
            return false;
        }
        return hashed.equals(hash(raw));
    }

    public static boolean isDefault(SysUser u) {
        return u != null && Integer.valueOf(1).equals(u.getAdmin());
    }
}
