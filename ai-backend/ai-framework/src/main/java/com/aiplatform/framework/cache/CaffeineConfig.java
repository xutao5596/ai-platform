package com.aiplatform.framework.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 本地缓存配置。
 */
@Configuration
@EnableCaching
public class CaffeineConfig {

    public static final String CACHE_DICT = "dict";
    public static final String CACHE_USER = "user";
    public static final String CACHE_PERMISSION = "permission";
    public static final String CACHE_PROJECT = "project";
    public static final String CACHE_DEFAULT = "default";

    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager();
        mgr.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(10_000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .recordStats());
        mgr.setAllowNullValues(false);
        return mgr;
    }
}
