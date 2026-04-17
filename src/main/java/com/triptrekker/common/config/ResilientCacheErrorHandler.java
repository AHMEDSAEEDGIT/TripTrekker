package com.triptrekker.common.config;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

@Slf4j
class ResilientCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(@NonNull RuntimeException exception, Cache cache, @NonNull Object key) {
        log.warn("Cache '{}': failed to get entry with key '{}' — falling back to API call. Cause: {}",
                cache.getName(), key, exception.getMessage(), exception);
    }

    @Override
    public void handleCachePutError(@NonNull RuntimeException exception, Cache cache, @NonNull Object key, Object value) {
        log.warn("Cache '{}': failed to put entry with key '{}' — result will not be cached. Cause: {}",
                cache.getName(), key, exception.getMessage(), exception);
    }

    @Override
    public void handleCacheEvictError(@NonNull RuntimeException exception, Cache cache, @NonNull Object key) {
        log.warn("Cache '{}': failed to evict entry with key '{}'. Cause: {}",
                cache.getName(), key, exception.getMessage(), exception);
    }

    @Override
    public void handleCacheClearError(@NonNull RuntimeException exception, Cache cache) {
        log.warn("Cache '{}': failed to clear. Cause: {}",
                cache.getName(), exception.getMessage(), exception);
    }
}