package com.hyhavenworld.core.config;

import com.typesafe.config.Config;

/**
 * Configuration for caching layer.
 * Caching is optional and disabled by default.
 */
public class CacheConfig {

    private final boolean enabled;
    private final long ttlSeconds;
    private final long maxSize;

    public CacheConfig(Config config) {
        // Caching is disabled by default
        this.enabled = config.hasPath("caching.enabled") && config.getBoolean("caching.enabled");

        if (enabled) {
            // Default: 5 minutes TTL
            this.ttlSeconds = config.hasPath("caching.ttl")
                ? config.getLong("caching.ttl")
                : 300;

            // Default: 1000 entries max
            this.maxSize = config.hasPath("caching.maxSize")
                ? config.getLong("caching.maxSize")
                : 1000;
        } else {
            this.ttlSeconds = 0;
            this.maxSize = 0;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public long getMaxSize() {
        return maxSize;
    }

    @Override
    public String toString() {
        if (!enabled) {
            return "CacheConfig{disabled}";
        }
        return "CacheConfig{" +
            "enabled=true" +
            ", ttlSeconds=" + ttlSeconds +
            ", maxSize=" + maxSize +
            '}';
    }
}