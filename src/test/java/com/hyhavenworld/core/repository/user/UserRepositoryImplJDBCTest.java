package com.hyhavenworld.core.repository.user;

import com.hyhavenworld.core.database.CorePersistenceException;
import com.hyhavenworld.core.database.StorageManager;
import com.hyhavenworld.core.domain.Permission;
import com.hyhavenworld.core.domain.Role;
import com.hyhavenworld.core.domain.User;
import com.hyhavenworld.core.repository.role.RoleRepositoryImplJDBC;
import com.hyhavenworld.core.test.TestDatabaseManager;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for UserRepositoryImplJDBC using an in-memory H2 database.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserRepositoryImplJDBCTest {

    private static TestDatabaseManager testDb;
    private UserRepositoryImplJDBC userRepository;
    private RoleRepositoryImplJDBC roleRepository;

    @BeforeAll
    static void setupDatabase() {
        testDb = TestDatabaseManager.getInstance();

        // Create a mock DatabaseManager that uses the test database
        com.hyhavenworld.core.database.DatabaseManager mockDb = new com.hyhavenworld.core.database.DatabaseManager() {
            @Override
            public java.sql.Connection getConnection() throws java.sql.SQLException {
                return testDb.getConnection();
            }
        };

        // Inject the mock DatabaseManager into StorageManager
        StorageManager.setInstanceForTesting(mockDb);
    }

    @BeforeEach
    void setup() {
        testDb.cleanDatabase();
        userRepository = new UserRepositoryImplJDBC();
        roleRepository = new RoleRepositoryImplJDBC();
    }

    @AfterAll
    static void tearDown() {
        StorageManager.resetForTesting();
        TestDatabaseManager.reset();
    }

    // ===== CRUD Tests =====

    @Test
    @Order(1)
    @DisplayName("Should create a new user")
    void testCreateUser() {
        UUID uuid = UUID.randomUUID();
        User user = new User(uuid.toString(), "testuser", null, null, 0L, Set.of(), Set.of());

        User created = userRepository.create(user);

        assertNotNull(created);
        assertEquals(uuid.toString(), created.getUuid());
        assertEquals("testuser", created.getUsername());
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getLastSeen());
        assertEquals(0L, created.getHoursPlayed());
    }

    @Test
    @Order(2)
    @DisplayName("Should get user by UUID")
    void testGetUserByUUID() {
        UUID uuid = UUID.randomUUID();
        User user = new User(uuid.toString(), "testuser", null, null, 0L, Set.of(), Set.of());
        userRepository.create(user);

        Optional<User> retrieved = userRepository.getUser(uuid);

        assertTrue(retrieved.isPresent());
        assertEquals("testuser", retrieved.get().getUsername());
    }

    @Test
    @Order(3)
    @DisplayName("Should get user by username")
    void testGetUserByUsername() {
        UUID uuid = UUID.randomUUID();
        User user = new User(uuid.toString(), "testuser", null, null, 0L, Set.of(), Set.of());
        userRepository.create(user);

        Optional<User> retrieved = userRepository.getUserByUsername("testuser");

        assertTrue(retrieved.isPresent());
        assertEquals(uuid.toString(), retrieved.get().getUuid());
    }

    @Test
    @Order(4)
    @DisplayName("Should update user information")
    void testUpdateUser() {
        UUID uuid = UUID.randomUUID();
        User user = new User(uuid.toString(), "testuser", null, null, 0L, Set.of(), Set.of());
        User created = userRepository.create(user);

        created.setUsername("updateduser");
        created.setHoursPlayed(100L);
        userRepository.update(created);

        Optional<User> updated = userRepository.getUser(uuid);
        assertTrue(updated.isPresent());
        assertEquals("updateduser", updated.get().getUsername());
        assertEquals(100L, updated.get().getHoursPlayed());
    }

    @Test
    @Order(5)
    @DisplayName("Should delete user")
    void testDeleteUser() {
        UUID uuid = UUID.randomUUID();
        User user = new User(uuid.toString(), "testuser", null, null, 0L, Set.of(), Set.of());
        userRepository.create(user);

        userRepository.delete(uuid);

        Optional<User> deleted = userRepository.getUser(uuid);
        assertFalse(deleted.isPresent());
    }

    @Test
    @Order(6)
    @DisplayName("Should find all users")
    void testFindAllUsers() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        userRepository.create(new User(uuid1.toString(), "user1", null, null, 0L, Set.of(), Set.of()));
        userRepository.create(new User(uuid2.toString(), "user2", null, null, 0L, Set.of(), Set.of()));

        Set<User> users = userRepository.findAll();

        assertEquals(2, users.size());
    }

    // ===== Role Management Tests =====

    @Test
    @Order(7)
    @DisplayName("Should add role to user")
    void testAddRole() {
        UUID uuid = UUID.randomUUID();
        userRepository.create(new User(uuid.toString(), "testuser", null, null, 0L, Set.of(), Set.of()));

        Role role = new Role(null, "admin", null, Set.of());
        roleRepository.create(role);

        userRepository.addRole(uuid, "admin");

        Set<Role> roles = userRepository.getRoles(uuid);
        assertEquals(1, roles.size());
        assertTrue(roles.stream().anyMatch(r -> r.getName().equals("admin")));
    }

    @Test
    @Order(8)
    @DisplayName("Should remove role from user")
    void testRemoveRole() {
        UUID uuid = UUID.randomUUID();
        userRepository.create(new User(uuid.toString(), "testuser", null, null, 0L, Set.of(), Set.of()));

        Role role = new Role(null, "admin", null, Set.of());
        roleRepository.create(role);

        userRepository.addRole(uuid, "admin");
        userRepository.removeRole(uuid, "admin");

        Set<Role> roles = userRepository.getRoles(uuid);
        assertEquals(0, roles.size());
    }

    @Test
    @Order(9)
    @DisplayName("Should get all roles for user")
    void testGetRoles() {
        UUID uuid = UUID.randomUUID();
        userRepository.create(new User(uuid.toString(), "testuser", null, null, 0L, Set.of(), Set.of()));

        roleRepository.create(new Role(null, "admin", null, Set.of()));
        roleRepository.create(new Role(null, "moderator", null, Set.of()));

        userRepository.addRole(uuid, "admin");
        userRepository.addRole(uuid, "moderator");

        Set<Role> roles = userRepository.getRoles(uuid);
        assertEquals(2, roles.size());
    }

    // ===== Permission Management Tests =====

    @Test
    @Order(10)
    @DisplayName("Should add permission to user")
    void testAddPermission() {
        UUID uuid = UUID.randomUUID();
        userRepository.create(new User(uuid.toString(), "testuser", null, null, 0L, Set.of(), Set.of()));

        userRepository.addPermission(uuid, "hyhavenworld.admin", true);

        Set<Permission> permissions = userRepository.getPermissions(uuid);
        assertEquals(1, permissions.size());
        assertTrue(permissions.stream().anyMatch(p ->
            p.getPermissionNode().equals("hyhavenworld.admin") && p.getValue()
        ));
    }

    @Test
    @Order(11)
    @DisplayName("Should update permission value on conflict")
    void testUpdatePermissionOnConflict() {
        UUID uuid = UUID.randomUUID();
        userRepository.create(new User(uuid.toString(), "testuser", null, null, 0L, Set.of(), Set.of()));

        userRepository.addPermission(uuid, "hyhavenworld.fly", true);
        userRepository.addPermission(uuid, "hyhavenworld.fly", false);

        Set<Permission> permissions = userRepository.getPermissions(uuid);
        assertEquals(1, permissions.size());
        Permission perm = permissions.iterator().next();
        assertEquals("hyhavenworld.fly", perm.getPermissionNode());
        assertFalse(perm.getValue());
    }

    @Test
    @Order(12)
    @DisplayName("Should remove permission from user")
    void testRemovePermission() {
        UUID uuid = UUID.randomUUID();
        userRepository.create(new User(uuid.toString(), "testuser", null, null, 0L, Set.of(), Set.of()));

        userRepository.addPermission(uuid, "hyhavenworld.admin", true);
        userRepository.removePermission(uuid, "hyhavenworld.admin");

        Set<Permission> permissions = userRepository.getPermissions(uuid);
        assertEquals(0, permissions.size());
    }

    @Test
    @Order(13)
    @DisplayName("Should get all permissions for user")
    void testGetPermissions() {
        UUID uuid = UUID.randomUUID();
        userRepository.create(new User(uuid.toString(), "testuser", null, null, 0L, Set.of(), Set.of()));

        userRepository.addPermission(uuid, "hyhavenworld.admin", true);
        userRepository.addPermission(uuid, "hyhavenworld.fly", false);
        userRepository.addPermission(uuid, "hyhavenworld.build", true);

        Set<Permission> permissions = userRepository.getPermissions(uuid);
        assertEquals(3, permissions.size());
    }

    // ===== Activity Update Tests =====

    @Test
    @Order(14)
    @DisplayName("Should update last seen timestamp")
    void testUpdateLastSeen() {
        UUID uuid = UUID.randomUUID();
        userRepository.create(new User(uuid.toString(), "testuser", null, null, 0L, Set.of(), Set.of()));

        LocalDateTime now = LocalDateTime.now();
        userRepository.updateLastSeen(uuid, now);

        Optional<User> user = userRepository.getUser(uuid);
        assertTrue(user.isPresent());
        assertNotNull(user.get().getLastSeen());
    }

    @Test
    @Order(15)
    @DisplayName("Should update hours played")
    void testUpdateHoursPlayed() {
        UUID uuid = UUID.randomUUID();
        userRepository.create(new User(uuid.toString(), "testuser", null, null, 0L, Set.of(), Set.of()));

        userRepository.updateHoursPlayed(uuid, 150L);

        Optional<User> user = userRepository.getUser(uuid);
        assertTrue(user.isPresent());
        assertEquals(150L, user.get().getHoursPlayed());
    }

    // ===== Error Cases =====

    @Test
    @Order(16)
    @DisplayName("Should throw exception when updating non-existent user")
    void testUpdateNonExistentUser() {
        UUID uuid = UUID.randomUUID();
        User user = new User(uuid.toString(), "testuser", LocalDateTime.now(), LocalDateTime.now(), 0L, Set.of(), Set.of());

        assertThrows(CorePersistenceException.class, () -> {
            userRepository.update(user);
        });
    }

    @Test
    @Order(17)
    @DisplayName("Should throw exception when deleting non-existent user")
    void testDeleteNonExistentUser() {
        UUID uuid = UUID.randomUUID();

        assertThrows(CorePersistenceException.class, () -> {
            userRepository.delete(uuid);
        });
    }
}