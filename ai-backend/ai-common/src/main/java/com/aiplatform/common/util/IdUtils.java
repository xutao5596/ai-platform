package com.aiplatform.common.util;

import lombok.experimental.UtilityClass;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ID 生成工具。
 */
@UtilityClass
public class IdUtils {

    private final AtomicLong SEQ = new AtomicLong(1);

    public String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public long nextId() {
        return SEQ.incrementAndGet();
    }

    public String randomCode(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }
}
