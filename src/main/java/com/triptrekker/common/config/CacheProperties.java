package com.triptrekker.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

@ConfigurationProperties("cache")
record CacheProperties(Duration defaultTtl, Map<String, Duration> caches) {

    CacheProperties {
        if (defaultTtl == null) defaultTtl = Duration.ofHours(1);
        if (caches == null) caches = Map.of();
    }

    Duration ttlFor(String cacheName) {
        return caches.getOrDefault(cacheName, defaultTtl);
    }
}