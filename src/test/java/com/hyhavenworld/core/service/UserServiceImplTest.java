package com.hyhavenworld.core.service;

import com.hyhavenworld.core.cache.CacheManager;
import com.hyhavenworld.core.config.CoreConfig;
import com.hyhavenworld.core.config.StorageType;
import com.hyhavenworld.core.database.CorePersistenceException;
import com.hyhavenworld.core.database.StorageManager;
import com.hyhavenworld.core.domain.User;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for UserServiceImpl testing createUser functionality.
 */
class UserServiceImplTest {

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
        tempYamlFile = Files.createTempFile("user-service-test", ".yml");
    }

    @BeforeEach
    void setup() throws IOException {
        // Clean database
        testDb.cleanDatabase();

        // Recreate temp YAML file
        Files.deleteIfExists(tempYamlFile);
        tempYamlFile = Files.createTempFile("user-service-test", ".yml");
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

    private UserServiceImpl createUserService(StorageType storageType, boolean cacheEnabled) throws IOException {
        createTestConfig(storageType, cacheEnabled);
        CoreConfig config = new CoreConfig();
        return new UserServiceImpl(config);
    }

    // ===== Basic Creation Tests =====

    @ParameterizedTest
    @EnumSource(StorageType.class)
    @DisplayName("Should create user with DATABASE and FILE storage")
    void testCreateUserWithBothStorageTypes(StorageType storageType) throws IOException {
        UserServiceImpl userService = createUserService(storageType, true);
        UUID uuid = UUID.randomUUID();
        String username = "testuser";

        User created = userService.createUser(uuid, username);

        assertNotNull(created);
        assertEquals(uuid.toString(), created.getUuid());
        assertEquals(username, created.getUsername());
    }

    @Test
    @DisplayName("Should return complete User object")
    void testCreateUserReturnsCompleteUser() throws IOException {
        UserServiceImpl userService = createUserService(StorageType.DATABASE, true);
        UUID uuid = UUID.randomUUID();

        User created = userService.createUser(uuid, "testuser");

        assertNotNull(created);
        assertNotNull(created.getUuid());
        assertNotNull(created.getUsername());
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getLastSeen());
        assertNotNull(created.getHoursPlayed());
        assertNotNull(created.getRoles());
        assertNotNull(created.getPermissions());
    }

    // ===== Field Validation Tests =====

    @Test
    @DisplayName("Should set current timestamps on creation")
    void testCreatedUserHasCurrentTimestamps() throws IOException {
        UserServiceImpl userService = createUserService(StorageType.DATABASE, true);
        UUID uuid = UUID.randomUUID();
        LocalDateTime before = LocalDateTime.now().minusSeconds(5);

        User created = userService.createUser(uuid, "testuser");

        LocalDateTime after = LocalDateTime.now().plusSeconds(5);

        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getLastSeen());
        assertTrue(created.getCreatedAt().isAfter(before) || created.getCreatedAt().isEqual(before));
        assertTrue(created.getCreatedAt().isBefore(after) || created.getCreatedAt().isEqual(after));
        assertTrue(created.getLastSeen().isAfter(before) || created.getLastSeen().isEqual(before));
        assertTrue(created.getLastSeen().isBefore(after) || created.getLastSeen().isEqual(after));
    }

    @Test
    @DisplayName("Should initialize hours played to zero")
    void testCreatedUserHasZeroHours() throws IOException {
        UserServiceImpl userService = createUserService(StorageType.DATABASE, true);
        UUID uuid = UUID.randomUUID();

        User created = userService.createUser(uuid, "testuser");

        assertEquals(0L, created.getHoursPlayed());
    }

    @Test
    @DisplayName("Should initialize roles as empty set")
    void testCreatedUserHasEmptyRoles() throws IOException {
        UserServiceImpl userService = createUserService(StorageType.DATABASE, true);
        UUID uuid = UUID.randomUUID();

        User created = userService.createUser(uuid, "testuser");

        assertNotNull(created.getRoles());
        assertTrue(created.getRoles().isEmpty());
    }

    @Test
    @DisplayName("Should initialize permissions as empty set")
    void testCreatedUserHasEmptyPermissions() throws IOException {
        UserServiceImpl userService = createUserService(StorageType.DATABASE, true);
        UUID uuid = UUID.randomUUID();

        User created = userService.createUser(uuid, "testuser");

        assertNotNull(created.getPermissions());
        assertTrue(created.getPermissions().isEmpty());
    }

    @Test
    @DisplayName("Should preserve UUID passed by caller")
    void testCreatedUserPreservesUUID() throws IOException {
        UserServiceImpl userService = createUserService(StorageType.DATABASE, true);
        UUID uuid = UUID.randomUUID();

        User created = userService.createUser(uuid, "testuser");

        assertEquals(uuid.toString(), created.getUuid());
    }

    @Test
    @DisplayName("Should preserve username passed by caller")
    void testCreatedUserPreservesUsername() throws IOException {
        UserServiceImpl userService = createUserService(StorageType.DATABASE, true);
        UUID uuid = UUID.randomUUID();
        String username = "mytestuser123";

        User created = userService.createUser(uuid, username);

        assertEquals(username, created.getUsername());
    }

    // ===== Cache Behavior Tests =====

    @Test
    @DisplayName("Should cache user after creation")
    void testCreateUserPopulatesCache() throws IOException {
        UserServiceImpl userService = createUserService(StorageType.DATABASE, true);
        UUID uuid = UUID.randomUUID();

        userService.createUser(uuid, "testuser");

        // Access cache directly through package-private method
        CacheManager cacheManager = userService.getCacheManager();
        User cached = cacheManager.getUserFromCache(uuid);

        assertNotNull(cached);
        assertEquals(uuid.toString(), cached.getUuid());
    }

    @Test
    @DisplayName("Should retrieve created user from cache on subsequent getUser call")
    void testCreateUserThenGetFromCache() throws IOException {
        UserServiceImpl userService = createUserService(StorageType.DATABASE, true);
        UUID uuid = UUID.randomUUID();

        userService.createUser(uuid, "testuser");

        // Clear database to ensure we're reading from cache
        testDb.cleanDatabase();

        // This should still work because of cache
        var retrieved = userService.getUser(uuid);

        assertTrue(retrieved.isPresent());
        assertEquals("testuser", retrieved.get().getUsername());
    }

    @Test
    @DisplayName("Should not cache when cache is disabled")
    void testCreateUserWithCacheDisabled() throws IOException {
        UserServiceImpl userService = createUserService(StorageType.DATABASE, false);
        UUID uuid = UUID.randomUUID();

        userService.createUser(uuid, "testuser");

        CacheManager cacheManager = userService.getCacheManager();
        User cached = cacheManager.getUserFromCache(uuid);

        assertNull(cached);
    }

    // ===== Error Handling Tests =====

    @ParameterizedTest
    @EnumSource(StorageType.class)
    @DisplayName("Should throw CorePersistenceException on duplicate UUID")
    void testCreateUserDuplicateUUID(StorageType storageType) throws IOException {
        UserServiceImpl userService = createUserService(storageType, true);
        UUID uuid = UUID.randomUUID();

        userService.createUser(uuid, "testuser1");

        assertThrows(CorePersistenceException.class, () -> {
            userService.createUser(uuid, "testuser2");
        });
    }

    // ===== Integration Tests =====

    @Test
    @DisplayName("Should be able to add role to newly created user")
    void testCreateUserThenAddRole() throws IOException {
        UserServiceImpl userService = createUserService(StorageType.DATABASE, true);

        // Create a role first
        RoleServiceImpl roleService = new RoleServiceImpl(new CoreConfig());

        com.hyhavenworld.core.domain.Role role = new com.hyhavenworld.core.domain.Role();
        role.setName("admin");
        role.setCreatedAt(LocalDateTime.now());
        role.setPermissions(java.util.Set.of());
        roleService.createRole(role);

        // Create user
        UUID uuid = UUID.randomUUID();
        User created = userService.createUser(uuid, "testuser");

        // Add role
        assertDoesNotThrow(() -> userService.addRole(uuid, "admin"));

        // Verify role was added
        var roles = userService.getRoles(uuid);
        assertEquals(1, roles.size());
    }

    @Test
    @DisplayName("Should be able to retrieve user by UUID after creation")
    void testCreateUserThenRetrieveByUUID() throws IOException {
        UserServiceImpl userService = createUserService(StorageType.DATABASE, true);
        UUID uuid = UUID.randomUUID();

        userService.createUser(uuid, "testuser");

        var retrieved = userService.getUser(uuid);

        assertTrue(retrieved.isPresent());
        assertEquals(uuid.toString(), retrieved.get().getUuid());
        assertEquals("testuser", retrieved.get().getUsername());
    }

    @Test
    @DisplayName("Should be able to create multiple users in sequence")
    void testCreateMultipleUsers() throws IOException {
        UserServiceImpl userService = createUserService(StorageType.DATABASE, true);

        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();

        User user1 = userService.createUser(uuid1, "user1");
        User user2 = userService.createUser(uuid2, "user2");
        User user3 = userService.createUser(uuid3, "user3");

        assertNotNull(user1);
        assertNotNull(user2);
        assertNotNull(user3);

        assertEquals("user1", user1.getUsername());
        assertEquals("user2", user2.getUsername());
        assertEquals("user3", user3.getUsername());
    }

    @Test
    @DisplayName("Should handle special characters in username")
    void testCreateUserWithSpecialCharactersInUsername() throws IOException {
        UserServiceImpl userService = createUserService(StorageType.DATABASE, true);
        UUID uuid = UUID.randomUUID();
        String username = "test_user-123.abc";

        User created = userService.createUser(uuid, username);

        assertEquals(username, created.getUsername());
    }

    @Test
    @DisplayName("Should handle long usernames")
    void testCreateUserWithLongUsername() throws IOException {
        UserServiceImpl userService = createUserService(StorageType.DATABASE, true);
        UUID uuid = UUID.randomUUID();
        String username = "a".repeat(50); // 50 character username

        User created = userService.createUser(uuid, username);

        assertEquals(username, created.getUsername());
    }

    // ===== Permission Management Tests =====

    @ParameterizedTest
    @EnumSource(StorageType.class)
    @DisplayName("Should add permission to user")
    void testAddPermissionToUser(StorageType storageType) throws IOException {
        UserServiceImpl userService = createUserService(storageType, true);
        UUID uuid = UUID.randomUUID();

        // Create user first
        userService.createUser(uuid, "testuser");

        // Add permission
        assertDoesNotThrow(() -> userService.addPermission(uuid, "hyhavenworld.fly", true));

        // Verify permission was added
        var permissions = userService.getPermissions(uuid);
        assertEquals(1, permissions.size());

        var permission = permissions.iterator().next();
        assertEquals("hyhavenworld.fly", permission.getPermissionNode());
        assertTrue(permission.getValue());
    }

    @ParameterizedTest
    @EnumSource(StorageType.class)
    @DisplayName("Should add deny permission to user")
    void testAddDenyPermissionToUser(StorageType storageType) throws IOException {
        UserServiceImpl userService = createUserService(storageType, true);
        UUID uuid = UUID.randomUUID();

        // Create user first
        userService.createUser(uuid, "testuser");

        // Add deny permission
        userService.addPermission(uuid, "hyhavenworld.admin", false);

        // Verify permission was added with deny value
        var permissions = userService.getPermissions(uuid);
        assertEquals(1, permissions.size());

        var permission = permissions.iterator().next();
        assertEquals("hyhavenworld.admin", permission.getPermissionNode());
        assertFalse(permission.getValue());
    }

    @Test
    @DisplayName("Should remove permission from user")
    void testRemovePermissionFromUser() throws IOException {
        UserServiceImpl userService = createUserService(StorageType.DATABASE, true);
        UUID uuid = UUID.randomUUID();

        // Create user and add permission
        userService.createUser(uuid, "testuser");
        userService.addPermission(uuid, "hyhavenworld.fly", true);

        // Verify permission exists
        assertEquals(1, userService.getPermissions(uuid).size());

        // Remove permission
        userService.removePermission(uuid, "hyhavenworld.fly");

        // Verify permission was removed
        var permissions = userService.getPermissions(uuid);
        assertTrue(permissions.isEmpty());
    }

    @Test
    @DisplayName("Should get empty permissions for new user")
    void testGetPermissionsForNewUser() throws IOException {
        UserServiceImpl userService = createUserService(StorageType.DATABASE, true);
        UUID uuid = UUID.randomUUID();

        // Create user
        userService.createUser(uuid, "testuser");

        // Verify no permissions
        var permissions = userService.getPermissions(uuid);
        assertNotNull(permissions);
        assertTrue(permissions.isEmpty());
    }

    @Test
    @DisplayName("Should handle multiple permissions on user")
    void testMultiplePermissionsOnUser() throws IOException {
        UserServiceImpl userService = createUserService(StorageType.DATABASE, true);
        UUID uuid = UUID.randomUUID();

        // Create user
        userService.createUser(uuid, "testuser");

        // Add multiple permissions
        userService.addPermission(uuid, "hyhavenworld.fly", true);
        userService.addPermission(uuid, "hyhavenworld.teleport", true);
        userService.addPermission(uuid, "hyhavenworld.admin", false);

        // Verify all permissions exist
        var permissions = userService.getPermissions(uuid);
        assertEquals(3, permissions.size());

        // Verify specific permissions
        var permissionNodes = permissions.stream()
            .map(com.hyhavenworld.core.domain.Permission::getPermissionNode)
            .toList();

        assertTrue(permissionNodes.contains("hyhavenworld.fly"));
        assertTrue(permissionNodes.contains("hyhavenworld.teleport"));
        assertTrue(permissionNodes.contains("hyhavenworld.admin"));
    }

    @Test
    @DisplayName("Should update permission value when adding same permission twice")
    void testUpdatePermissionValue() throws IOException {
        UserServiceImpl userService = createUserService(StorageType.DATABASE, true);
        UUID uuid = UUID.randomUUID();

        // Create user
        userService.createUser(uuid, "testuser");

        // Add permission with true value
        userService.addPermission(uuid, "hyhavenworld.fly", true);
        var permissions1 = userService.getPermissions(uuid);
        assertEquals(1, permissions1.size());
        assertTrue(permissions1.iterator().next().getValue());

        // Update same permission with false value
        userService.addPermission(uuid, "hyhavenworld.fly", false);
        var permissions2 = userService.getPermissions(uuid);
        assertEquals(1, permissions2.size());
        assertFalse(permissions2.iterator().next().getValue());
    }

    @Test
    @DisplayName("Should invalidate cache when adding permission")
    void testAddPermissionInvalidatesCache() throws IOException {
        UserServiceImpl userService = createUserService(StorageType.DATABASE, true);
        UUID uuid = UUID.randomUUID();

        // Create user (this caches it)
        userService.createUser(uuid, "testuser");

        // Verify user is cached
        CacheManager cacheManager = userService.getCacheManager();
        assertNotNull(cacheManager.getUserFromCache(uuid));

        // Add permission (should invalidate cache)
        userService.addPermission(uuid, "hyhavenworld.fly", true);

        // Verify cache was invalidated
        assertNull(cacheManager.getUserFromCache(uuid));
    }

    @Test
    @DisplayName("Should invalidate cache when removing permission")
    void testRemovePermissionInvalidatesCache() throws IOException {
        UserServiceImpl userService = createUserService(StorageType.DATABASE, true);
        UUID uuid = UUID.randomUUID();

        // Create user and add permission
        userService.createUser(uuid, "testuser");
        userService.addPermission(uuid, "hyhavenworld.fly", true);

        // Get user to cache it
        userService.getUser(uuid);

        // Verify user is cached
        CacheManager cacheManager = userService.getCacheManager();
        assertNotNull(cacheManager.getUserFromCache(uuid));

        // Remove permission (should invalidate cache)
        userService.removePermission(uuid, "hyhavenworld.fly");

        // Verify cache was invalidated
        assertNull(cacheManager.getUserFromCache(uuid));
    }

    @Test
    @DisplayName("Should handle permission operations on non-existent user gracefully")
    void testPermissionOperationsOnNonExistentUser() throws IOException {
        UserServiceImpl userService = createUserService(StorageType.DATABASE, true);
        UUID uuid = UUID.randomUUID();

        // Try to get permissions for non-existent user - should return empty set or handle gracefully
        var permissions = userService.getPermissions(uuid);
        assertNotNull(permissions);
        assertTrue(permissions.isEmpty());
    }
}
