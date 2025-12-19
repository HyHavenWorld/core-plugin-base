package com.hyhavenworld.core.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CacheConfigTest {

    @Test
    void testDefaultDisabled() {
        String configStr = """
            # No caching config
            """;
        Config config = ConfigFactory.parseString(configStr);
        CacheConfig cacheConfig = new CacheConfig(config);

        assertFalse(cacheConfig.isEnabled());
        assertEquals(0, cacheConfig.getTtlSeconds());
        assertEquals(0, cacheConfig.getMaxSize());
    }

    @Test
    void testEnabledWithDefaults() {
        String configStr = """
            caching {
                enabled = true
            }
            """;
        Config config = ConfigFactory.parseString(configStr);
        CacheConfig cacheConfig = new CacheConfig(config);

        assertTrue(cacheConfig.isEnabled());
        assertEquals(300, cacheConfig.getTtlSeconds()); // Default 5 minutes
        assertEquals(1000, cacheConfig.getMaxSize()); // Default 1000
    }

    @Test
    void testCustomConfiguration() {
        String configStr = """
            caching {
                enabled = true
                ttl = 600
                maxSize = 5000
            }
            """;
        Config config = ConfigFactory.parseString(configStr);
        CacheConfig cacheConfig = new CacheConfig(config);

        assertTrue(cacheConfig.isEnabled());
        assertEquals(600, cacheConfig.getTtlSeconds());
        assertEquals(5000, cacheConfig.getMaxSize());
    }

    @Test
    void testExplicitlyDisabled() {
        String configStr = """
            caching {
                enabled = false
                ttl = 600
                maxSize = 5000
            }
            """;
        Config config = ConfigFactory.parseString(configStr);
        CacheConfig cacheConfig = new CacheConfig(config);

        assertFalse(cacheConfig.isEnabled());
        assertEquals(0, cacheConfig.getTtlSeconds());
        assertEquals(0, cacheConfig.getMaxSize());
    }

    @Test
    void testToStringWhenDisabled() {
        String configStr = """
            caching {
                enabled = false
            }
            """;
        Config config = ConfigFactory.parseString(configStr);
        CacheConfig cacheConfig = new CacheConfig(config);

        String str = cacheConfig.toString();
        assertTrue(str.contains("disabled"));
    }

    @Test
    void testToStringWhenEnabled() {
        String configStr = """
            caching {
                enabled = true
                ttl = 300
                maxSize = 1000
            }
            """;
        Config config = ConfigFactory.parseString(configStr);
        CacheConfig cacheConfig = new CacheConfig(config);

        String str = cacheConfig.toString();
        assertTrue(str.contains("enabled=true"));
        assertTrue(str.contains("ttlSeconds=300"));
        assertTrue(str.contains("maxSize=1000"));
    }

    @Test
    void testMinimalTTL() {
        String configStr = """
            caching {
                enabled = true
                ttl = 1
                maxSize = 10
            }
            """;
        Config config = ConfigFactory.parseString(configStr);
        CacheConfig cacheConfig = new CacheConfig(config);

        assertTrue(cacheConfig.isEnabled());
        assertEquals(1, cacheConfig.getTtlSeconds());
        assertEquals(10, cacheConfig.getMaxSize());
    }

    @Test
    void testLargeTTL() {
        String configStr = """
            caching {
                enabled = true
                ttl = 86400
                maxSize = 100000
            }
            """;
        Config config = ConfigFactory.parseString(configStr);
        CacheConfig cacheConfig = new CacheConfig(config);

        assertTrue(cacheConfig.isEnabled());
        assertEquals(86400, cacheConfig.getTtlSeconds()); // 24 hours
        assertEquals(100000, cacheConfig.getMaxSize());
    }
}
