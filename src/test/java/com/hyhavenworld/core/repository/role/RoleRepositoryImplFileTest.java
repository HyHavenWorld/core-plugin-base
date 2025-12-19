package com.hyhavenworld.core.repository.role;

import com.hyhavenworld.core.database.CorePersistenceException;
import com.hyhavenworld.core.domain.Permission;
import com.hyhavenworld.core.domain.Role;
import com.hyhavenworld.core.domain.User;
import com.hyhavenworld.core.repository.user.UserRepository;
import com.hyhavenworld.core.repository.user.UserRepositoryImplFile;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RoleRepositoryImplFileTest {

    private Path tempFile;
    private RoleRepository roleRepository;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() throws IOException {
        tempFile = Files.createTempFile("test-data-", ".yml");
        roleRepository = new RoleRepositoryImplFile(tempFile.toString());
        userRepository = new UserRepositoryImplFile(tempFile.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempFile != null && Files.exists(tempFile)) {
            Files.delete(tempFile);
        }
    }

    // ===== CRUD Tests =====

    @Test
    void testCreateRole() throws CorePersistenceException {
        Role role = new Role(null, "admin", LocalDateTime.now(), Set.of());

        Role created = roleRepository.create(role);

        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("admin", created.getName());
        assertNotNull(created.getCreatedAt());
    }

    @Test
    void testCreateDuplicateRole() throws CorePersistenceException {
        Role role = new Role(null, "admin", LocalDateTime.now(), Set.of());
        roleRepository.create(role);

        assertThrows(CorePersistenceException.class, () -> roleRepository.create(role));
    }

    @Test
    void testGetById() throws CorePersistenceException {
        Role role = new Role(null, "admin", LocalDateTime.now(), Set.of());
        Role created = roleRepository.create(role);

        Optional<Role> retrieved = roleRepository.getById(created.getId());

        assertTrue(retrieved.isPresent());
        assertEquals(created.getId(), retrieved.get().getId());
        assertEquals("admin", retrieved.get().getName());
    }

    @Test
    void testGetByIdNotFound() throws CorePersistenceException {
        Optional<Role> retrieved = roleRepository.getById(999L);

        assertFalse(retrieved.isPresent());
    }

    @Test
    void testGetByName() throws CorePersistenceException {
        Role role = new Role(null, "admin", LocalDateTime.now(), Set.of());
        roleRepository.create(role);

        Optional<Role> retrieved = roleRepository.getByName("admin");

        assertTrue(retrieved.isPresent());
        assertEquals("admin", retrieved.get().getName());
    }

    @Test
    void testGetByNameNotFound() throws CorePersistenceException {
        Optional<Role> retrieved = roleRepository.getByName("nonexistent");

        assertFalse(retrieved.isPresent());
    }

    @Test
    void testUpdateRole() throws CorePersistenceException {
        Role role = new Role(null, "admin", LocalDateTime.now(), Set.of());
        Role created = roleRepository.create(role);

        Role updated = new Role(created.getId(), "super_admin", created.getCreatedAt(), Set.of());
        roleRepository.update(updated);

        Optional<Role> retrieved = roleRepository.getById(created.getId());
        assertTrue(retrieved.isPresent());
        assertEquals("super_admin", retrieved.get().getName());
    }

    @Test
    void testUpdateRoleUpdatesReferences() throws CorePersistenceException {
        // Create role
        Role role = new Role(null, "admin", LocalDateTime.now(), Set.of());
        Role created = roleRepository.create(role);

        // Create user with role
        UUID uuid = UUID.randomUUID();
        User user = new User(uuid.toString(), "testuser", LocalDateTime.now(), LocalDateTime.now(), 0L, Set.of(), Set.of());
        userRepository.create(user);
        userRepository.addRole(uuid, "admin");

        // Create child role with parent
        Role childRole = new Role(null, "moderator", LocalDateTime.now(), Set.of());
        roleRepository.create(childRole);
        roleRepository.addInheritance("admin", "moderator");

        // Update role name
        Role updated = new Role(created.getId(), "super_admin", created.getCreatedAt(), Set.of());
        roleRepository.update(updated);

        // Verify user role updated
        Set<Role> userRoles = userRepository.getRoles(uuid);
        assertEquals(1, userRoles.size());
        assertEquals("super_admin", userRoles.iterator().next().getName());

        // Verify inheritance updated
        Set<Role> parents = roleRepository.getParentRoles("moderator");
        assertEquals(1, parents.size());
        assertEquals("super_admin", parents.iterator().next().getName());
    }

    @Test
    void testUpdateNonexistentRole() {
        Role role = new Role(999L, "admin", LocalDateTime.now(), Set.of());

        assertThrows(CorePersistenceException.class, () -> roleRepository.update(role));
    }

    @Test
    void testDeleteRole() throws CorePersistenceException {
        Role role = new Role(null, "admin", LocalDateTime.now(), Set.of());
        Role created = roleRepository.create(role);

        roleRepository.delete(created.getId());

        Optional<Role> retrieved = roleRepository.getById(created.getId());
        assertFalse(retrieved.isPresent());
    }

    @Test
    void testDeleteRoleCleansUpReferences() throws CorePersistenceException {
        // Create role
        Role role = new Role(null, "admin", LocalDateTime.now(), Set.of());
        Role created = roleRepository.create(role);

        // Create user with role
        UUID uuid = UUID.randomUUID();
        User user = new User(uuid.toString(), "testuser", LocalDateTime.now(), LocalDateTime.now(), 0L, Set.of(), Set.of());
        userRepository.create(user);
        userRepository.addRole(uuid, "admin");

        // Create child role
        Role childRole = new Role(null, "moderator", LocalDateTime.now(), Set.of());
        roleRepository.create(childRole);
        roleRepository.addInheritance("admin", "moderator");

        // Delete role
        roleRepository.delete(created.getId());

        // Verify user role removed
        Set<Role> userRoles = userRepository.getRoles(uuid);
        assertEquals(0, userRoles.size());

        // Verify inheritance removed
        Set<Role> parents = roleRepository.getParentRoles("moderator");
        assertEquals(0, parents.size());
    }

    @Test
    void testDeleteNonexistentRole() {
        assertThrows(CorePersistenceException.class, () -> roleRepository.delete(999L));
    }

    @Test
    void testDeleteByName() throws CorePersistenceException {
        Role role = new Role(null, "admin", LocalDateTime.now(), Set.of());
        roleRepository.create(role);

        roleRepository.deleteByName("admin");

        Optional<Role> retrieved = roleRepository.getByName("admin");
        assertFalse(retrieved.isPresent());
    }

    @Test
    void testDeleteByNameNonexistent() {
        assertThrows(CorePersistenceException.class, () -> roleRepository.deleteByName("nonexistent"));
    }

    @Test
    void testFindAll() throws CorePersistenceException {
        Role role1 = new Role(null, "admin", LocalDateTime.now(), Set.of());
        Role role2 = new Role(null, "moderator", LocalDateTime.now(), Set.of());

        roleRepository.create(role1);
        roleRepository.create(role2);

        Set<Role> allRoles = roleRepository.findAll();

        assertEquals(2, allRoles.size());
    }

    // ===== Permission Management Tests =====

    @Test
    void testAddPermission() throws CorePersistenceException {
        Role role = new Role(null, "admin", LocalDateTime.now(), Set.of());
        roleRepository.create(role);

        roleRepository.addPermission("admin", "test.permission", true);

        Set<Permission> permissions = roleRepository.getPermissions("admin");
        assertEquals(1, permissions.size());
        Permission perm = permissions.iterator().next();
        assertEquals("test.permission", perm.getPermissionNode());
        assertTrue(perm.getValue());
    }

    @Test
    void testAddPermissionToNonexistentRole() {
        assertThrows(CorePersistenceException.class,
            () -> roleRepository.addPermission("nonexistent", "test.permission", true));
    }

    @Test
    void testRemovePermission() throws CorePersistenceException {
        Role role = new Role(null, "admin", LocalDateTime.now(), Set.of());
        roleRepository.create(role);
        roleRepository.addPermission("admin", "test.permission", true);

        roleRepository.removePermission("admin", "test.permission");

        Set<Permission> permissions = roleRepository.getPermissions("admin");
        assertEquals(0, permissions.size());
    }

    @Test
    void testRemovePermissionFromNonexistentRole() {
        assertThrows(CorePersistenceException.class,
            () -> roleRepository.removePermission("nonexistent", "test.permission"));
    }

    @Test
    void testGetPermissions() throws CorePersistenceException {
        Role role = new Role(null, "admin", LocalDateTime.now(), Set.of());
        roleRepository.create(role);
        roleRepository.addPermission("admin", "permission1", true);
        roleRepository.addPermission("admin", "permission2", false);

        Set<Permission> permissions = roleRepository.getPermissions("admin");

        assertEquals(2, permissions.size());
    }

    @Test
    void testGetPermissionsForNonexistentRole() throws CorePersistenceException {
        Set<Permission> permissions = roleRepository.getPermissions("nonexistent");

        assertEquals(0, permissions.size());
    }

    // ===== Inheritance Tests =====

    @Test
    void testAddInheritance() throws CorePersistenceException {
        Role parent = new Role(null, "admin", LocalDateTime.now(), Set.of());
        Role child = new Role(null, "moderator", LocalDateTime.now(), Set.of());
        roleRepository.create(parent);
        roleRepository.create(child);

        roleRepository.addInheritance("admin", "moderator");

        Set<Role> parents = roleRepository.getParentRoles("moderator");
        assertEquals(1, parents.size());
        assertEquals("admin", parents.iterator().next().getName());
    }

    @Test
    void testAddInheritanceNonexistentChild() throws CorePersistenceException {
        Role parent = new Role(null, "admin", LocalDateTime.now(), Set.of());
        roleRepository.create(parent);

        assertThrows(CorePersistenceException.class,
            () -> roleRepository.addInheritance("admin", "nonexistent"));
    }

    @Test
    void testAddInheritanceNonexistentParent() throws CorePersistenceException {
        Role child = new Role(null, "moderator", LocalDateTime.now(), Set.of());
        roleRepository.create(child);

        assertThrows(CorePersistenceException.class,
            () -> roleRepository.addInheritance("nonexistent", "moderator"));
    }

    @Test
    void testRemoveInheritance() throws CorePersistenceException {
        Role parent = new Role(null, "admin", LocalDateTime.now(), Set.of());
        Role child = new Role(null, "moderator", LocalDateTime.now(), Set.of());
        roleRepository.create(parent);
        roleRepository.create(child);
        roleRepository.addInheritance("admin", "moderator");

        roleRepository.removeInheritance("admin", "moderator");

        Set<Role> parents = roleRepository.getParentRoles("moderator");
        assertEquals(0, parents.size());
    }

    @Test
    void testGetParentRoles() throws CorePersistenceException {
        Role parent1 = new Role(null, "admin", LocalDateTime.now(), Set.of());
        Role parent2 = new Role(null, "senior", LocalDateTime.now(), Set.of());
        Role child = new Role(null, "moderator", LocalDateTime.now(), Set.of());
        roleRepository.create(parent1);
        roleRepository.create(parent2);
        roleRepository.create(child);
        roleRepository.addInheritance("admin", "moderator");
        roleRepository.addInheritance("senior", "moderator");

        Set<Role> parents = roleRepository.getParentRoles("moderator");

        assertEquals(2, parents.size());
    }

    @Test
    void testGetChildRoles() throws CorePersistenceException {
        Role parent = new Role(null, "admin", LocalDateTime.now(), Set.of());
        Role child1 = new Role(null, "moderator", LocalDateTime.now(), Set.of());
        Role child2 = new Role(null, "helper", LocalDateTime.now(), Set.of());
        roleRepository.create(parent);
        roleRepository.create(child1);
        roleRepository.create(child2);
        roleRepository.addInheritance("admin", "moderator");
        roleRepository.addInheritance("admin", "helper");

        Set<Role> children = roleRepository.getChildRoles("admin");

        assertEquals(2, children.size());
    }

    // ===== Effective Permissions Tests =====

    @Test
    void testGetEffectivePermissionsNoInheritance() throws CorePersistenceException {
        Role role = new Role(null, "admin", LocalDateTime.now(), Set.of());
        roleRepository.create(role);
        roleRepository.addPermission("admin", "permission1", true);
        roleRepository.addPermission("admin", "permission2", false);

        Set<Permission> effective = roleRepository.getEffectivePermissions("admin");

        assertEquals(2, effective.size());
    }

    @Test
    void testGetEffectivePermissionsWithInheritance() throws CorePersistenceException {
        // Create parent role with permissions
        Role parent = new Role(null, "admin", LocalDateTime.now(), Set.of());
        roleRepository.create(parent);
        roleRepository.addPermission("admin", "admin.permission", true);

        // Create child role with permissions
        Role child = new Role(null, "moderator", LocalDateTime.now(), Set.of());
        roleRepository.create(child);
        roleRepository.addPermission("moderator", "mod.permission", true);

        // Add inheritance
        roleRepository.addInheritance("admin", "moderator");

        // Get effective permissions
        Set<Permission> effective = roleRepository.getEffectivePermissions("moderator");

        assertEquals(2, effective.size());
        assertTrue(effective.stream().anyMatch(p -> p.getPermissionNode().equals("admin.permission")));
        assertTrue(effective.stream().anyMatch(p -> p.getPermissionNode().equals("mod.permission")));
    }

    @Test
    void testGetEffectivePermissionsWithMultiLevelInheritance() throws CorePersistenceException {
        // Create grandparent role
        Role grandparent = new Role(null, "owner", LocalDateTime.now(), Set.of());
        roleRepository.create(grandparent);
        roleRepository.addPermission("owner", "owner.permission", true);

        // Create parent role
        Role parent = new Role(null, "admin", LocalDateTime.now(), Set.of());
        roleRepository.create(parent);
        roleRepository.addPermission("admin", "admin.permission", true);

        // Create child role
        Role child = new Role(null, "moderator", LocalDateTime.now(), Set.of());
        roleRepository.create(child);
        roleRepository.addPermission("moderator", "mod.permission", true);

        // Add inheritance chain
        roleRepository.addInheritance("owner", "admin");
        roleRepository.addInheritance("admin", "moderator");

        // Get effective permissions
        Set<Permission> effective = roleRepository.getEffectivePermissions("moderator");

        assertEquals(3, effective.size());
        assertTrue(effective.stream().anyMatch(p -> p.getPermissionNode().equals("owner.permission")));
        assertTrue(effective.stream().anyMatch(p -> p.getPermissionNode().equals("admin.permission")));
        assertTrue(effective.stream().anyMatch(p -> p.getPermissionNode().equals("mod.permission")));
    }

    @Test
    void testGetEffectivePermissionsWithCircularInheritance() throws CorePersistenceException {
        // Create roles
        Role role1 = new Role(null, "role1", LocalDateTime.now(), Set.of());
        Role role2 = new Role(null, "role2", LocalDateTime.now(), Set.of());
        roleRepository.create(role1);
        roleRepository.create(role2);

        // Add permissions
        roleRepository.addPermission("role1", "permission1", true);
        roleRepository.addPermission("role2", "permission2", true);

        // Create circular inheritance (manually in YAML for testing)
        roleRepository.addInheritance("role1", "role2");
        roleRepository.addInheritance("role2", "role1");

        // Should not cause infinite loop
        Set<Permission> effective = roleRepository.getEffectivePermissions("role1");

        // Should get both permissions without stack overflow
        assertEquals(2, effective.size());
    }
}