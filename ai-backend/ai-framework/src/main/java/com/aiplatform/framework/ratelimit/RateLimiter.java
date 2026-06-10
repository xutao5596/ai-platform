package com.aiplatform.framework.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.aiplatform.framework.exception.RateLimitException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于 Caffeine 的内存限流器(每分钟固定窗口)。
 * key = apiKeyId, value = 窗口内请求计数;窗口长度 1 分钟。
 * 计数超阈值抛 RateLimitException。
 */
public class RateLimiter {

    private final int defaultLimit;
    private final Cache<Long, AtomicInteger> counters;

    public RateLimiter() {
        this(60);
    }

    public RateLimiter(int defaultLimit) {
        this.defaultLimit = defaultLimit;
        this.counters = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(50_000)
                .build();
    }

    public int getDefaultLimit() {
        return defaultLimit;
    }

    /**
     * 校验并递增计数。
     *
     * @param apiKeyId API Key 主键(0 表示匿名/未识别,不做限流)
     * @param limit    该 key 限流阈值(<=0 用 defaultLimit)
     * @throws RateLimitException 超限时抛出
     */
    public void check(Long apiKeyId, int limit) {
        if (apiKeyId == null || apiKeyId <= 0) {
            return;
        }
        int effective = limit > 0 ? limit : defaultLimit;
        AtomicInteger counter = counters.get(apiKeyId, k -> new AtomicInteger(0));
        int n = counter.incrementAndGet();
        if (n > effective) {
            throw new RateLimitException("请求过于频繁,请稍后再试", 60L);
        }
    }

    /**
     * 重置某个 key 的计数(用于管理接口 / 测试)。
     */
    public void reset(Long apiKeyId) {
        if (apiKeyId != null) {
            counters.invalidate(apiKeyId);
        }
    }

    /**
     * 查看当前计数(用于监控/调试)。
     */
    public int current(Long apiKeyId) {
        if (apiKeyId == null) return 0;
        AtomicInteger c = counters.getIfPresent(apiKeyId);
        return c == null ? 0 : c.get();
    }
}
