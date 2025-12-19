package com.hyhavenworld.core.repository.user;

import com.hyhavenworld.core.database.CorePersistenceException;
import com.hyhavenworld.core.domain.Permission;
import com.hyhavenworld.core.domain.Role;
import com.hyhavenworld.core.domain.User;
import com.hyhavenworld.core.repository.role.RoleRepository;
import com.hyhavenworld.core.repository.role.RoleRepositoryImplFile;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryImplFileTest {

    private Path tempFile;
    private UserRepository userRepository;
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() throws IOException {
        tempFile = Files.createTempFile("test-data-", ".yml");
        userRepository = new UserRepositoryImplFile(tempFile.toString());
        roleRepository = new RoleRepositoryImplFile(tempFile.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempFile != null && Files.exists(tempFile)) {
            Files.delete(tempFile);
        }
    }

    // ===== CRUD Tests =====

    @Test
    void testCreateUser() throws CorePersistenceException {
        UUID uuid = UUID.randomUUID();
        User user = new User(uuid.toString(), "testuser", LocalDateTime.now(), LocalDateTime.now(), 0L, Set.of(), Set.of());

        User created = userRepository.create(user);

        assertNotNull(created);
        assertEquals(uuid.toString(), created.getUuid());
        assertEquals("testuser", created.getUsername());
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getLastSeen());
        assertEquals(0L, created.getHoursPlayed());
    }

    @Test
    void testCreateDuplicateUser() throws CorePersistenceException {
        UUID uuid = UUID.randomUUID();
        User user = new User(uuid.toString(), "testuser", LocalDateTime.now(), LocalDateTime.now(), 0L, Set.of(), Set.of());

        userRepository.create(user);

        assertThrows(CorePersistenceException.class, () -> userRepository.create(user));
    }

    @Test
    void testGetUser() throws CorePersistenceException {
        UUID uuid = UUID.randomUUID();
        User user = new User(uuid.toString(), "testuser", LocalDateTime.now(), LocalDateTime.now(), 0L, Set.of(), Set.of());
        userRepository.create(user);

        Optional<User> retrieved = userRepository.getUser(uuid);

        assertTrue(retrieved.isPresent());
        assertEquals(uuid.toString(), retrieved.get().getUuid());
        assertEquals("testuser", retrieved.get().getUsername());
    }

    @Test
    void testGetUserNotFound() throws CorePersistenceException {
        UUID uuid = UUID.randomUUID();

        Optional<User> retrieved = userRepository.getUser(uuid);

        assertFalse(retrieved.isPresent());
    }

    @Test
    void testGetUserByUsername() throws CorePersistenceException {
        UUID uuid = UUID.randomUUID();
        User user = new User(uuid.toString(), "testuser", LocalDateTime.now(), LocalDateTime.now(), 0L, Set.of(), Set.of());
        userRepository.create(user);

        Optional<User> retrieved = userRepository.getUserByUsername("testuser");

        assertTrue(retrieved.isPresent());
        assertEquals(uuid.toString(), retrieved.get().getUuid());
        assertEquals("testuser", retrieved.get().getUsername());
    }

    @Test
    void testGetUserByUsernameNotFound() throws CorePersistenceException {
        Optional<User> retrieved = userRepository.getUserByUsername("nonexistent");

        assertFalse(retrieved.isPresent());
    }

    @Test
    void testUpdateUser() throws CorePersistenceException {
        UUID uuid = UUID.randomUUID();
        User user = new User(uuid.toString(), "testuser", LocalDateTime.now(), LocalDateTime.now(), 0L, Set.of(), Set.of());
        userRepository.create(user);

        LocalDateTime newLastSeen = LocalDateTime.now().plusDays(1);
        User updated = new User(uuid.toString(), "updateduser", user.getCreatedAt(), newLastSeen, 100L, Set.of(), Set.of());
        userRepository.update(updated);

        Optional<User> retrieved = userRepository.getUser(uuid);
        assertTrue(retrieved.isPresent());
        assertEquals("updateduser", retrieved.get().getUsername());
        assertEquals(100L, retrieved.get().getHoursPlayed());
    }

    @Test
    void testUpdateNonexistentUser() {
        UUID uuid = UUID.randomUUID();
        User user = new User(uuid.toString(), "testuser", LocalDateTime.now(), LocalDateTime.now(), 0L, Set.of(), Set.of());

        assertThrows(CorePersistenceException.class, () -> userRepository.update(user));
    }

    @Test
    void testDeleteUser() throws CorePersistenceException {
        UUID uuid = UUID.randomUUID();
        User user = new User(uuid.toString(), "testuser", LocalDateTime.now(), LocalDateTime.now(), 0L, Set.of(), Set.of());
        userRepository.create(user);

        userRepository.delete(uuid);

        Optional<User> retrieved = userRepository.getUser(uuid);
        assertFalse(retrieved.isPresent());
    }

    @Test
    void testDeleteNonexistentUser() {
        UUID uuid = UUID.randomUUID();

        assertThrows(CorePersistenceException.class, () -> userRepository.delete(uuid));
    }

    @Test
    void testFindAll() throws CorePersistenceException {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        User user1 = new User(uuid1.toString(), "user1", LocalDateTime.now(), LocalDateTime.now(), 0L, Set.of(), Set.of());
        User user2 = new User(uuid2.toString(), "user2", LocalDateTime.now(), LocalDateTime.now(), 0L, Set.of(), Set.of());

        userRepository.create(user1);
        userRepository.create(user2);

        Set<User> allUsers = userRepository.findAll();

        assertEquals(2, allUsers.size());
    }

    // ===== Role Management Tests =====

    @Test
    void testAddRole() throws CorePersistenceException {
        // Create role
        Role role = new Role(null, "admin", LocalDateTime.now(), Set.of());
        roleRepository.create(role);

        // Create user
        UUID uuid = UUID.randomUUID();
        User user = new User(uuid.toString(), "testuser", LocalDateTime.now(), LocalDateTime.now(), 0L, Set.of(), Set.of());
        userRepository.create(user);

        // Add role to user
        userRepository.addRole(uuid, "admin");

        // Verify
        Set<Role> roles = userRepository.getRoles(uuid);
        assertEquals(1, roles.size());
        assertEquals("admin", roles.iterator().next().getName());
    }

    @Test
    void testRemoveRole() throws CorePersistenceException {
        // Create role
        Role role = new Role(null, "admin", LocalDateTime.now(), Set.of());
        roleRepository.create(role);

        // Create user and add role
        UUID uuid = UUID.randomUUID();
        User user = new User(uuid.toString(), "testuser", LocalDateTime.now(), LocalDateTime.now(), 0L, Set.of(), Set.of());
        userRepository.create(user);
        userRepository.addRole(uuid, "admin");

        // Remove role
        userRepository.removeRole(uuid, "admin");

        // Verify
        Set<Role> roles = userRepository.getRoles(uuid);
        assertEquals(0, roles.size());
    }

    @Test
    void testGetRoles() throws CorePersistenceException {
        // Create roles
        Role role1 = new Role(null, "admin", LocalDateTime.now(), Set.of());
        Role role2 = new Role(null, "moderator", LocalDateTime.now(), Set.of());
        roleRepository.create(role1);
        roleRepository.create(role2);

        // Create user and add roles
        UUID uuid = UUID.randomUUID();
        User user = new User(uuid.toString(), "testuser", LocalDateTime.now(), LocalDateTime.now(), 0L, Set.of(), Set.of());
        userRepository.create(user);
        userRepository.addRole(uuid, "admin");
        userRepository.addRole(uuid, "moderator");

        // Verify
        Set<Role> roles = userRepository.getRoles(uuid);
        assertEquals(2, roles.size());
    }

    // ===== Permission Management Tests =====

    @Test
    void testAddPermission() throws CorePersistenceException {
        UUID uuid = UUID.randomUUID();
        User user = new User(uuid.toString(), "testuser", LocalDateTime.now(), LocalDateTime.now(), 0L, Set.of(), Set.of());
        userRepository.create(user);

        userRepository.addPermission(uuid, "test.permission", true);

        Set<Permission> permissions = userRepository.getPermissions(uuid);
        assertEquals(1, permissions.size());
        Permission perm = permissions.iterator().next();
        assertEquals("test.permission", perm.getPermissionNode());
        assertTrue(perm.getValue());
    }

    @Test
    void testRemovePermission() throws CorePersistenceException {
        UUID uuid = UUID.randomUUID();
        User user = new User(uuid.toString(), "testuser", LocalDateTime.now(), LocalDateTime.now(), 0L, Set.of(), Set.of());
        userRepository.create(user);
        userRepository.addPermission(uuid, "test.permission", true);

        userRepository.removePermission(uuid, "test.permission");

        Set<Permission> permissions = userRepository.getPermissions(uuid);
        assertEquals(0, permissions.size());
    }

    @Test
    void testGetPermissions() throws CorePersistenceException {
        UUID uuid = UUID.randomUUID();
        User user = new User(uuid.toString(), "testuser", LocalDateTime.now(), LocalDateTime.now(), 0L, Set.of(), Set.of());
        userRepository.create(user);
        userRepository.addPermission(uuid, "permission1", true);
        userRepository.addPermission(uuid, "permission2", false);

        Set<Permission> permissions = userRepository.getPermissions(uuid);

        assertEquals(2, permissions.size());
    }

    // ===== Activity Update Tests =====

    @Test
    void testUpdateLastSeen() throws CorePersistenceException {
        UUID uuid = UUID.randomUUID();
        User user = new User(uuid.toString(), "testuser", LocalDateTime.now(), LocalDateTime.now(), 0L, Set.of(), Set.of());
        userRepository.create(user);

        LocalDateTime newTimestamp = LocalDateTime.now().plusDays(1);
        userRepository.updateLastSeen(uuid, newTimestamp);

        Optional<User> retrieved = userRepository.getUser(uuid);
        assertTrue(retrieved.isPresent());
        assertEquals(newTimestamp.withNano(0), retrieved.get().getLastSeen().withNano(0));
    }

    @Test
    void testUpdateHoursPlayed() throws CorePersistenceException {
        UUID uuid = UUID.randomUUID();
        User user = new User(uuid.toString(), "testuser", LocalDateTime.now(), LocalDateTime.now(), 0L, Set.of(), Set.of());
        userRepository.create(user);

        userRepository.updateHoursPlayed(uuid, 150L);

        Optional<User> retrieved = userRepository.getUser(uuid);
        assertTrue(retrieved.isPresent());
        assertEquals(150L, retrieved.get().getHoursPlayed());
    }
}