package com.hyhavenworld.core.cache;

import com.hyhavenworld.core.config.CacheConfig;
import com.hyhavenworld.core.domain.Role;
import com.hyhavenworld.core.domain.User;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CacheManagerTest {

    private CacheManager cacheManager;
    private CacheConfig enabledConfig;
    private CacheConfig disabledConfig;

    @BeforeEach
    void setUp() {
        // Config with caching enabled
        String enabledConfigStr = """
            caching {
                enabled = true
                ttl = 5
                maxSize = 10
            }
            """;
        Config enabledConf = ConfigFactory.parseString(enabledConfigStr);
        enabledConfig = new CacheConfig(enabledConf);

        // Config with caching disabled
        String disabledConfigStr = """
            caching {
                enabled = false
            }
            """;
        Config disabledConf = ConfigFactory.parseString(disabledConfigStr);
        disabledConfig = new CacheConfig(disabledConf);
    }

    @Test
    void testCacheDisabled() {
        CacheManager manager = new CacheManager(disabledConfig);
        assertFalse(manager.isEnabled());

        UUID uuid = UUID.randomUUID();
        User user = createTestUser(uuid);

        // Should not cache when disabled
        manager.cacheUser(uuid, user);
        assertNull(manager.getUserFromCache(uuid));
    }

    @Test
    void testCacheEnabled() {
        CacheManager manager = new CacheManager(enabledConfig);
        assertTrue(manager.isEnabled());
    }

    @Test
    void testUserCacheOperations() {
        CacheManager manager = new CacheManager(enabledConfig);
        UUID uuid = UUID.randomUUID();
        User user = createTestUser(uuid);

        // Initially empty
        assertNull(manager.getUserFromCache(uuid));

        // Cache user
        manager.cacheUser(uuid, user);
        User cached = manager.getUserFromCache(uuid);
        assertNotNull(cached);
        assertEquals(uuid.toString(), cached.getUuid());
        assertEquals("testuser", cached.getUsername());

        // Invalidate
        manager.invalidateUser(uuid);
        assertNull(manager.getUserFromCache(uuid));
    }

    @Test
    void testRoleCacheOperations() {
        CacheManager manager = new CacheManager(enabledConfig);
        String roleName = "admin";
        Role role = createTestRole(roleName);

        // Initially empty
        assertNull(manager.getRoleFromCache(roleName));

        // Cache role
        manager.cacheRole(roleName, role);
        Role cached = manager.getRoleFromCache(roleName);
        assertNotNull(cached);
        assertEquals(roleName, cached.getName());

        // Invalidate
        manager.invalidateRole(roleName);
        assertNull(manager.getRoleFromCache(roleName));
    }

    @Test
    void testInvalidateAll() {
        CacheManager manager = new CacheManager(enabledConfig);

        // Cache some users and roles
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        manager.cacheUser(uuid1, createTestUser(uuid1));
        manager.cacheUser(uuid2, createTestUser(uuid2));
        manager.cacheRole("admin", createTestRole("admin"));
        manager.cacheRole("moderator", createTestRole("moderator"));

        // Verify cached
        assertNotNull(manager.getUserFromCache(uuid1));
        assertNotNull(manager.getUserFromCache(uuid2));
        assertNotNull(manager.getRoleFromCache("admin"));
        assertNotNull(manager.getRoleFromCache("moderator"));

        // Invalidate all
        manager.invalidateAll();

        // Verify all invalidated
        assertNull(manager.getUserFromCache(uuid1));
        assertNull(manager.getUserFromCache(uuid2));
        assertNull(manager.getRoleFromCache("admin"));
        assertNull(manager.getRoleFromCache("moderator"));
    }

    @Test
    void testCacheStats() {
        CacheManager manager = new CacheManager(enabledConfig);
        UUID uuid = UUID.randomUUID();
        User user = createTestUser(uuid);

        // Initially empty stats
        CacheManager.CacheStats stats = manager.getStats();
        assertEquals(0, stats.getSize());
        assertEquals(0, stats.getHits());
        assertEquals(0, stats.getMisses());

        // Cache user
        manager.cacheUser(uuid, user);

        // Hit
        manager.getUserFromCache(uuid);
        stats = manager.getStats();
        assertTrue(stats.getHits() > 0);

        // Miss
        manager.getUserFromCache(UUID.randomUUID());
        stats = manager.getStats();
        assertTrue(stats.getMisses() > 0);

        // Hit rate
        assertTrue(stats.getHitRate() > 0 && stats.getHitRate() <= 1.0);
    }

    @Test
    void testCacheStatsWhenDisabled() {
        CacheManager manager = new CacheManager(disabledConfig);
        CacheManager.CacheStats stats = manager.getStats();

        assertEquals(0, stats.getSize());
        assertEquals(0, stats.getHits());
        assertEquals(0, stats.getMisses());
        assertEquals(0, stats.getEvictions());
        assertEquals(0.0, stats.getHitRate());
    }

    @Test
    void testCacheTTL() throws InterruptedException {
        // Create config with very short TTL for testing
        String shortTtlConfig = """
            caching {
                enabled = true
                ttl = 1
                maxSize = 10
            }
            """;
        Config conf = ConfigFactory.parseString(shortTtlConfig);
        CacheConfig config = new CacheConfig(conf);
        CacheManager manager = new CacheManager(config);

        UUID uuid = UUID.randomUUID();
        User user = createTestUser(uuid);

        // Cache user
        manager.cacheUser(uuid, user);
        assertNotNull(manager.getUserFromCache(uuid));

        // Wait for TTL to expire
        Thread.sleep(1500);

        // Force cleanup to trigger TTL eviction
        manager.cleanUp();

        // Should be evicted
        assertNull(manager.getUserFromCache(uuid));
    }

    @Test
    void testCacheMaxSize() {
        CacheManager manager = new CacheManager(enabledConfig);

        // maxSize is 10, so add 15 users
        for (int i = 0; i < 15; i++) {
            UUID uuid = UUID.randomUUID();
            manager.cacheUser(uuid, createTestUser(uuid));
        }

        // Force cleanup to trigger eviction
        manager.cleanUp();

        // Should have evicted some entries
        CacheManager.CacheStats stats = manager.getStats();
        assertTrue(stats.getSize() <= 10);
        assertTrue(stats.getEvictions() > 0, "Should have evicted at least 5 entries");
    }

    @Test
    void testInvalidateAllUsers() {
        CacheManager manager = new CacheManager(enabledConfig);

        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        manager.cacheUser(uuid1, createTestUser(uuid1));
        manager.cacheUser(uuid2, createTestUser(uuid2));
        manager.cacheRole("admin", createTestRole("admin"));

        manager.invalidateAllUsers();

        assertNull(manager.getUserFromCache(uuid1));
        assertNull(manager.getUserFromCache(uuid2));
        // Role should still be cached
        assertNotNull(manager.getRoleFromCache("admin"));
    }

    @Test
    void testInvalidateAllRoles() {
        CacheManager manager = new CacheManager(enabledConfig);

        UUID uuid = UUID.randomUUID();
        manager.cacheUser(uuid, createTestUser(uuid));
        manager.cacheRole("admin", createTestRole("admin"));
        manager.cacheRole("moderator", createTestRole("moderator"));

        manager.invalidateAllRoles();

        // User should still be cached
        assertNotNull(manager.getUserFromCache(uuid));
        assertNull(manager.getRoleFromCache("admin"));
        assertNull(manager.getRoleFromCache("moderator"));
    }

    // Helper methods

    private User createTestUser(UUID uuid) {
        return new User(
            uuid.toString(),
            "testuser",
            LocalDateTime.now(),
            LocalDateTime.now(),
            0L,
            Set.of(),
            Set.of()
        );
    }

    private Role createTestRole(String name) {
        return new Role(
            1L,
            name,
            LocalDateTime.now(),
            Set.of()
        );
    }
}