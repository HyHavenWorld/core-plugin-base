package com.hyhavenworld.core;

import com.hyhavenworld.core.api.PermissionService;
import com.hyhavenworld.core.api.RoleService;
import com.hyhavenworld.core.api.UserService;
import com.hyhavenworld.core.config.CoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CorePluginManagerTest {

    @AfterEach
    void tearDown() {
        // Always shutdown after each test
        if (CorePluginManager.isInitialized()) {
            CorePluginManager.shutdown();
        }

        // Clean up test config file
        File configFile = new File("application.yml");
        if (configFile.exists()) {
            configFile.delete();
        }
    }

    @Test
    void testInitializeWithFileStorage() throws IOException {
        createTestConfig("FILE");

        CorePluginManager.initialize();

        assertTrue(CorePluginManager.isInitialized());
        assertNotNull(CorePluginManager.get());
        assertFalse(CorePluginManager.get().isDatabaseMode());
    }

    @Test
    void testGetServicesAfterInitialization() throws IOException {
        createTestConfig("FILE");

        CorePluginManager.initialize();

        UserService users = CorePluginManager.get().users();
        RoleService roles = CorePluginManager.get().roles();
        PermissionService permissions = CorePluginManager.get().permissions();

        assertNotNull(users);
        assertNotNull(roles);
        assertNotNull(permissions);
    }

    @Test
    void testShutdown() throws IOException {
        createTestConfig("FILE");

        CorePluginManager.initialize();
        assertTrue(CorePluginManager.isInitialized());

        CorePluginManager.shutdown();
        assertFalse(CorePluginManager.isInitialized());
    }

    @Test
    void testGetBeforeInitializeThrows() {
        assertThrows(IllegalStateException.class, () -> CorePluginManager.get());
    }

    @Test
    void testDoubleInitializeThrows() throws IOException {
        createTestConfig("FILE");

        CorePluginManager.initialize();

        assertThrows(IllegalStateException.class, () -> CorePluginManager.initialize());
    }

    @Test
    void testReinitializeAfterShutdown() throws IOException {
        createTestConfig("FILE");

        // First initialization
        CorePluginManager.initialize();
        assertTrue(CorePluginManager.isInitialized());

        // Shutdown
        CorePluginManager.shutdown();
        assertFalse(CorePluginManager.isInitialized());

        // Re-initialize should work
        CorePluginManager.initialize();
        assertTrue(CorePluginManager.isInitialized());
    }

    @Test
    void testGetConfigAfterInitialization() throws IOException {
        createTestConfig("FILE");

        CorePluginManager.initialize();

        CoreConfig config = CorePluginManager.get().getConfig();
        assertNotNull(config);
    }

    @Test
    void testIsDatabaseMode() throws IOException {
        createTestConfig("FILE");

        CorePluginManager.initialize();

        assertFalse(CorePluginManager.get().isDatabaseMode());
    }

    @Test
    void testShutdownMultipleTimesIsSafe() throws IOException {
        createTestConfig("FILE");

        CorePluginManager.initialize();
        CorePluginManager.shutdown();

        // Second shutdown should not throw
        assertDoesNotThrow(() -> CorePluginManager.shutdown());
    }

    @Test
    void testIsInitialized() throws IOException {
        assertFalse(CorePluginManager.isInitialized());

        createTestConfig("FILE");
        CorePluginManager.initialize();

        assertTrue(CorePluginManager.isInitialized());

        CorePluginManager.shutdown();

        assertFalse(CorePluginManager.isInitialized());
    }

    @Test
    void testInitializeWithCachingEnabled() throws IOException {
        createTestConfigWithCaching("FILE", true);

        CorePluginManager.initialize();

        assertTrue(CorePluginManager.isInitialized());
        assertNotNull(CorePluginManager.get().users());
    }

    @Test
    void testInitializeWithCachingDisabled() throws IOException {
        createTestConfigWithCaching("FILE", false);

        CorePluginManager.initialize();

        assertTrue(CorePluginManager.isInitialized());
        assertNotNull(CorePluginManager.get().users());
    }

    // Helper methods

    private void createTestConfig(String storageType) throws IOException {
        String config = String.format("""
            storageType = %s

            file {
              path = "test-data.yml"
            }

            database {
              host = localhost
              port = 5432
              name = test
              user = test
              pass = test
              maximumPoolSize = 10
            }

            caching {
              enabled = false
            }
            """, storageType);

        try (FileWriter writer = new FileWriter("application.yml")) {
            writer.write(config);
        }
    }

    private void createTestConfigWithCaching(String storageType, boolean cachingEnabled) throws IOException {
        String config = String.format("""
            storageType = %s

            file {
              path = "test-data.yml"
            }

            database {
              host = localhost
              port = 5432
              name = test
              user = test
              pass = test
              maximumPoolSize = 10
            }

            caching {
              enabled = %s
              ttl = 300
              maxSize = 1000
            }
            """, storageType, cachingEnabled);

        try (FileWriter writer = new FileWriter("application.yml")) {
            writer.write(config);
        }
    }
}