package com.hyhavenworld.core.service;

import com.hyhavenworld.core.cache.CacheManager;
import com.hyhavenworld.core.config.CoreConfig;
import com.hyhavenworld.core.config.StorageType;
import com.hyhavenworld.core.database.StorageManager;
import com.hyhavenworld.core.domain.Permission;
import com.hyhavenworld.core.domain.Role;
import com.hyhavenworld.core.test.TestDatabaseManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RoleServiceImpl testing permission management functionality.
 */
class RoleServiceImplTest {

    private static TestDatabaseManager testDb;
    private static Path tempYamlFile;

    @BeforeAll
    static void setupDatabase() throws IOException {
        // Setup database for JDBC tests
        testDb = TestDatabaseManager.getInstance();

        com.hyhavenworld.core.database.DatabaseManager mockDb = new com.hyhavenworld.core.database.DatabaseManager() {
            @Override
            public java.sql.Connection getConnection() throws java.sql.SQLException {
                return testDb.getConnection();
            }
        };

        StorageManager.setInstanceForTesting(mockDb);

        // Create temp YAML file for File tests
        tempYamlFile = Files.createTempFile("role-service-test", ".yml");
    }

    @BeforeEach
    void setup() throws IOException {
        // Clean database
        testDb.cleanDatabase();

        // Recreate temp YAML file
        Files.deleteIfExists(tempYamlFile);
        tempYamlFile = Files.createTempFile("role-service-test", ".yml");
    }

    @AfterAll
    static void tearDown() throws IOException {
        StorageManager.resetForTesting();
        TestDatabaseManager.reset();
        Files.deleteIfExists(tempYamlFile);

        // Clean up test config file
        File configFile = new File("application.conf");
        if (configFile.exists()) {
            configFile.delete();
        }
    }

    // ===== Helper Methods =====

    private void createTestConfig(StorageType storageType, boolean cacheEnabled) throws IOException {
        String config = String.format("""
            storageType = %s

            file {
              path = "%s"
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
            """, storageType.name(), tempYamlFile.toString().replace("\\", "\\\\"), cacheEnabled);

        try (FileWriter writer = new FileWriter("application.conf")) {
            writer.write(config);
        }
    }

    private RoleServiceImpl createRoleService(StorageType storageType, boolean cacheEnabled) throws IOException {
        createTestConfig(storageType, cacheEnabled);
        CoreConfig config = new CoreConfig();
        return new RoleServiceImpl(config);
    }

    private Role createTestRole(String name) {
        Role role = new Role();
        role.setName(name);
        role.setCreatedAt(LocalDateTime.now());
        role.setPermissions(Set.of());
        return role;
    }

    // ===== Permission Management Tests =====

    @ParameterizedTest
    @EnumSource(StorageType.class)
    @DisplayName("Should add permission to role with DATABASE and FILE storage")
    void testAddPermissionToRole(StorageType storageType) throws IOException {
        RoleServiceImpl roleService = createRoleService(storageType, true);

        // Create role first
        Role role = createTestRole("admin");
        roleService.createRole(role);

        // Add permission
        assertDoesNotThrow(() -> roleService.addPermission("admin", "hyhavenworld.fly", true));
    }

    @ParameterizedTest
    @EnumSource(StorageType.class)
    @DisplayName("Should add deny permission to role")
    void testAddDenyPermissionToRole(StorageType storageType) throws IOException {
        RoleServiceImpl roleService = createRoleService(storageType, true);

        // Create role first
        Role role = createTestRole("moderator");
        roleService.createRole(role);

        // Add deny permission
        assertDoesNotThrow(() -> roleService.addPermission("moderator", "hyhavenworld.admin", false));
    }

    @ParameterizedTest
    @EnumSource(StorageType.class)
    @DisplayName("Should remove permission from role with DATABASE and FILE storage")
    void testRemovePermissionFromRole(StorageType storageType) throws IOException {
        RoleServiceImpl roleService = createRoleService(storageType, true);

        // Create role and add permission
        Role role = createTestRole("admin");
        roleService.createRole(role);
        roleService.addPermission("admin", "hyhavenworld.fly", true);

        // Remove permission
        assertDoesNotThrow(() -> roleService.removePermission("admin", "hyhavenworld.fly"));
    }

    @Test
    @DisplayName("Should handle multiple permissions on role")
    void testMultiplePermissionsOnRole() throws IOException {
        RoleServiceImpl roleService = createRoleService(StorageType.DATABASE, true);

        // Create role
        Role role = createTestRole("admin");
        roleService.createRole(role);

        // Add multiple permissions
        roleService.addPermission("admin", "hyhavenworld.fly", true);
        roleService.addPermission("admin", "hyhavenworld.teleport", true);
        roleService.addPermission("admin", "hyhavenworld.admin", true);

        // All permissions should be added successfully
        assertDoesNotThrow(() -> {
            roleService.addPermission("admin", "hyhavenworld.spawn", false);
        });
    }

    @Test
    @DisplayName("Should update permission value when adding same permission twice")
    void testUpdatePermissionValue() throws IOException {
        RoleServiceImpl roleService = createRoleService(StorageType.DATABASE, true);

        // Create role
        Role role = createTestRole("admin");
        roleService.createRole(role);

        // Add permission with true value
        roleService.addPermission("admin", "hyhavenworld.fly", true);

        // Update same permission with false value - should not throw
        assertDoesNotThrow(() -> roleService.addPermission("admin", "hyhavenworld.fly", false));
    }

    // ===== Cache Invalidation Tests =====

    @Test
    @DisplayName("Should invalidate cache when adding permission")
    void testAddPermissionInvalidatesCache() throws IOException {
        RoleServiceImpl roleService = createRoleService(StorageType.DATABASE, true);

        // Create role
        Role role = createTestRole("admin");
        roleService.createRole(role);

        // Load role to cache it
        roleService.getRoleByName("admin");

        // Verify role is cached
        CacheManager cacheManager = roleService.getCacheManager();
        assertNotNull(cacheManager.getRoleFromCache("admin"));

        // Add permission (should invalidate cache)
        roleService.addPermission("admin", "hyhavenworld.fly", true);

        // Verify cache was invalidated
        assertNull(cacheManager.getRoleFromCache("admin"));
    }

    @Test
    @DisplayName("Should invalidate cache when removing permission")
    void testRemovePermissionInvalidatesCache() throws IOException {
        RoleServiceImpl roleService = createRoleService(StorageType.DATABASE, true);

        // Create role and add permission
        Role role = createTestRole("admin");
        roleService.createRole(role);
        roleService.addPermission("admin", "hyhavenworld.fly", true);

        // Load role to cache it
        roleService.getRoleByName("admin");

        // Verify role is cached
        CacheManager cacheManager = roleService.getCacheManager();
        assertNotNull(cacheManager.getRoleFromCache("admin"));

        // Remove permission (should invalidate cache)
        roleService.removePermission("admin", "hyhavenworld.fly");

        // Verify cache was invalidated
        assertNull(cacheManager.getRoleFromCache("admin"));
    }

    @Test
    @DisplayName("Should not cache when cache is disabled")
    void testPermissionOperationsWithCacheDisabled() throws IOException {
        RoleServiceImpl roleService = createRoleService(StorageType.DATABASE, false);

        // Create role and add permission
        Role role = createTestRole("admin");
        roleService.createRole(role);
        roleService.addPermission("admin", "hyhavenworld.fly", true);

        // Load role
        roleService.getRoleByName("admin");

        // Verify role is NOT cached
        CacheManager cacheManager = roleService.getCacheManager();
        assertNull(cacheManager.getRoleFromCache("admin"));
    }

    // ===== Integration Tests =====

    @Test
    @DisplayName("Should add permission then remove it")
    void testAddThenRemovePermission() throws IOException {
        RoleServiceImpl roleService = createRoleService(StorageType.DATABASE, true);

        // Create role
        Role role = createTestRole("admin");
        roleService.createRole(role);

        // Add permission
        roleService.addPermission("admin", "hyhavenworld.fly", true);

        // Remove permission
        assertDoesNotThrow(() -> roleService.removePermission("admin", "hyhavenworld.fly"));
    }

    @Test
    @DisplayName("Should handle removing non-existent permission gracefully")
    void testRemoveNonExistentPermission() throws IOException {
        RoleServiceImpl roleService = createRoleService(StorageType.DATABASE, true);

        // Create role
        Role role = createTestRole("admin");
        roleService.createRole(role);

        // Try to remove permission that doesn't exist - should not throw
        assertDoesNotThrow(() -> roleService.removePermission("admin", "hyhavenworld.nonexistent"));
    }

    @ParameterizedTest
    @EnumSource(StorageType.class)
    @DisplayName("Should handle permission operations with both storage types")
    void testPermissionOperationsWithBothStorageTypes(StorageType storageType) throws IOException {
        RoleServiceImpl roleService = createRoleService(storageType, true);

        // Create role
        Role role = createTestRole("testRole");
        roleService.createRole(role);

        // Add permission
        roleService.addPermission("testRole", "hyhavenworld.test", true);

        // Remove permission
        roleService.removePermission("testRole", "hyhavenworld.test");

        // Add it again
        assertDoesNotThrow(() -> roleService.addPermission("testRole", "hyhavenworld.test", false));
    }

    @Test
    @DisplayName("Should handle special characters in permission nodes")
    void testPermissionWithSpecialCharacters() throws IOException {
        RoleServiceImpl roleService = createRoleService(StorageType.DATABASE, true);

        // Create role
        Role role = createTestRole("admin");
        roleService.createRole(role);

        // Add permission with special characters
        String permissionNode = "hyhavenworld.region.*.edit";
        assertDoesNotThrow(() -> roleService.addPermission("admin", permissionNode, true));

        // Remove it
        assertDoesNotThrow(() -> roleService.removePermission("admin", permissionNode));
    }

    @Test
    @DisplayName("Should handle long permission nodes")
    void testLongPermissionNode() throws IOException {
        RoleServiceImpl roleService = createRoleService(StorageType.DATABASE, true);

        // Create role
        Role role = createTestRole("admin");
        roleService.createRole(role);

        // Add permission with long node
        String permissionNode = "hyhavenworld." + "a".repeat(100) + ".test";
        assertDoesNotThrow(() -> roleService.addPermission("admin", permissionNode, true));
    }
}
