package com.hyhavenworld.core.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hyhavenworld.core.config.CacheConfig;
import com.hyhavenworld.core.domain.Role;
import com.hyhavenworld.core.domain.User;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Manages in-memory caches for users and roles.
 * Caching is optional and configured via CacheConfig.
 */
public class CacheManager {

    private final Cache<UUID, User> userCache;
    private final Cache<String, Role> roleCache; // keyed by role name
    private final boolean enabled;

    public CacheManager(CacheConfig config) {
        this.enabled = config.isEnabled();

        if (enabled) {
            this.userCache = buildCache(config);
            this.roleCache = buildCache(config);
        } else {
            this.userCache = null;
            this.roleCache = null;
        }
    }

    private <K, V> Cache<K, V> buildCache(CacheConfig config) {
        return Caffeine.newBuilder()
            .expireAfterWrite(config.getTtlSeconds(), TimeUnit.SECONDS)
            .maximumSize(config.getMaxSize())
            .recordStats()
            .build();
    }

    // ===== User Cache Operations =====

    public User getUserFromCache(UUID uuid) {
        return enabled ? userCache.getIfPresent(uuid) : null;
    }

    public void cacheUser(UUID uuid, User user) {
        if (enabled) {
            userCache.put(uuid, user);
        }
    }

    public void invalidateUser(UUID uuid) {
        if (enabled) {
            userCache.invalidate(uuid);
        }
    }

    public void invalidateAllUsers() {
        if (enabled) {
            userCache.invalidateAll();
        }
    }

    // ===== Role Cache Operations =====

    public Role getRoleFromCache(String roleName) {
        return enabled ? roleCache.getIfPresent(roleName) : null;
    }

    public void cacheRole(String roleName, Role role) {
        if (enabled) {
            roleCache.put(roleName, role);
        }
    }

    public void invalidateRole(String roleName) {
        if (enabled) {
            roleCache.invalidate(roleName);
        }
    }

    public void invalidateAllRoles() {
        if (enabled) {
            roleCache.invalidateAll();
        }
    }

    // ===== General Operations =====

    public void invalidateAll() {
        if (enabled) {
            userCache.invalidateAll();
            roleCache.invalidateAll();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Force cache maintenance (cleanup expired entries and enforce size limits).
     * Primarily useful for testing to ensure evictions happen immediately.
     */
    public void cleanUp() {
        if (enabled) {
            userCache.cleanUp();
            roleCache.cleanUp();
        }
    }

    /**
     * Get cache statistics for monitoring.
     */
    public CacheStats getStats() {
        if (!enabled) {
            return new CacheStats(0, 0, 0, 0);
        }

        com.github.benmanes.caffeine.cache.stats.CacheStats userStats = userCache.stats();
        com.github.benmanes.caffeine.cache.stats.CacheStats roleStats = roleCache.stats();

        return new CacheStats(
            userCache.estimatedSize() + roleCache.estimatedSize(),
            userStats.hitCount() + roleStats.hitCount(),
            userStats.missCount() + roleStats.missCount(),
            userStats.evictionCount() + roleStats.evictionCount()
        );
    }

    public static class CacheStats {
        private final long size;
        private final long hits;
        private final long misses;
        private final long evictions;

        public CacheStats(long size, long hits, long misses, long evictions) {
            this.size = size;
            this.hits = hits;
            this.misses = misses;
            this.evictions = evictions;
        }

        public long getSize() { return size; }
        public long getHits() { return hits; }
        public long getMisses() { return misses; }
        public long getEvictions() { return evictions; }

        public double getHitRate() {
            long total = hits + misses;
            return total == 0 ? 0.0 : (double) hits / total;
        }

        @Override
        public String toString() {
            return String.format("CacheStats{size=%d, hits=%d, misses=%d, evictions=%d, hitRate=%.2f%%}",
                size, hits, misses, evictions, getHitRate() * 100);
        }
    }
}