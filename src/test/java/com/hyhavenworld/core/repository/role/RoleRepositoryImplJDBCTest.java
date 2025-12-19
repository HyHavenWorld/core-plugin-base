package com.hyhavenworld.core.repository.role;

import com.hyhavenworld.core.database.CorePersistenceException;
import com.hyhavenworld.core.database.StorageManager;
import com.hyhavenworld.core.domain.Permission;
import com.hyhavenworld.core.domain.Role;
import com.hyhavenworld.core.test.TestDatabaseManager;
import org.junit.jupiter.api.*;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RoleRepositoryImplJDBC using an in-memory H2 database.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RoleRepositoryImplJDBCTest {

    private static TestDatabaseManager testDb;
    private RoleRepositoryImplJDBC roleRepository;

    @BeforeAll
    static void setupDatabase() {
        testDb = TestDatabaseManager.getInstance();

        // Mock StorageManager to use test database
        try {
            var field = StorageManager.class.getDeclaredField("instance");
            field.setAccessible(true);

            var mockStorage = new StorageManager() {
                @Override
                public com.hyhavenworld.core.database.DatabaseManager getDatabase() {
                    return new com.hyhavenworld.core.database.DatabaseManager() {
                        @Override
                        public java.sql.Connection getConnection() throws java.sql.SQLException {
                            return testDb.getConnection();
                        }
                    };
                }
            };

            field.set(null, mockStorage);
        } catch (Exception e) {
            throw new RuntimeException("Failed to mock StorageManager", e);
        }
    }

    @BeforeEach
    void setup() {
        testDb.cleanDatabase();
        roleRepository = new RoleRepositoryImplJDBC();
    }

    @AfterAll
    static void tearDown() {
        TestDatabaseManager.reset();
    }

    // ===== CRUD Tests =====

    @Test
    @Order(1)
    @DisplayName("Should create a new role")
    void testCreateRole() {
        Role role = new Role(null, "admin", null, Set.of());

        Role created = roleRepository.create(role);

        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("admin", created.getName());
        assertNotNull(created.getCreatedAt());
    }

    @Test
    @Order(2)
    @DisplayName("Should get role by ID")
    void testGetRoleById() {
        Role role = new Role(null, "admin", null, Set.of());
        Role created = roleRepository.create(role);

        Optional<Role> retrieved = roleRepository.getById(created.getId());

        assertTrue(retrieved.isPresent());
        assertEquals("admin", retrieved.get().getName());
    }

    @Test
    @Order(3)
    @DisplayName("Should get role by name")
    void testGetRoleByName() {
        Role role = new Role(null, "moderator", null, Set.of());
        roleRepository.create(role);

        Optional<Role> retrieved = roleRepository.getByName("moderator");

        assertTrue(retrieved.isPresent());
        assertEquals("moderator", retrieved.get().getName());
    }

    @Test
    @Order(4)
    @DisplayName("Should update role name")
    void testUpdateRole() {
        Role role = new Role(null, "admin", null, Set.of());
        Role created = roleRepository.create(role);

        created.setName("super-admin");
        roleRepository.update(created);

        Optional<Role> updated = roleRepository.getById(created.getId());
        assertTrue(updated.isPresent());
        assertEquals("super-admin", updated.get().getName());
    }

    @Test
    @Order(5)
    @DisplayName("Should delete role by ID")
    void testDeleteRoleById() {
        Role role = new Role(null, "admin", null, Set.of());
        Role created = roleRepository.create(role);

        roleRepository.delete(created.getId());

        Optional<Role> deleted = roleRepository.getById(created.getId());
        assertFalse(deleted.isPresent());
    }

    @Test
    @Order(6)
    @DisplayName("Should delete role by name")
    void testDeleteRoleByName() {
        Role role = new Role(null, "admin", null, Set.of());
        roleRepository.create(role);

        roleRepository.deleteByName("admin");

        Optional<Role> deleted = roleRepository.getByName("admin");
        assertFalse(deleted.isPresent());
    }

    @Test
    @Order(7)
    @DisplayName("Should find all roles")
    void testFindAllRoles() {
        roleRepository.create(new Role(null, "admin", null, Set.of()));
        roleRepository.create(new Role(null, "moderator", null, Set.of()));
        roleRepository.create(new Role(null, "user", null, Set.of()));

        Set<Role> roles = roleRepository.findAll();

        assertEquals(3, roles.size());
    }

    // ===== Permission Management Tests =====

    @Test
    @Order(8)
    @DisplayName("Should add permission to role")
    void testAddPermission() {
        roleRepository.create(new Role(null, "admin", null, Set.of()));

        roleRepository.addPermission("admin", "hyhavenworld.admin", true);

        Set<Permission> permissions = roleRepository.getPermissions("admin");
        assertEquals(1, permissions.size());
        assertTrue(permissions.stream().anyMatch(p ->
            p.getPermissionNode().equals("hyhavenworld.admin") && p.getValue()
        ));
    }

    @Test
    @Order(9)
    @DisplayName("Should update permission value on conflict")
    void testUpdatePermissionOnConflict() {
        roleRepository.create(new Role(null, "admin", null, Set.of()));

        roleRepository.addPermission("admin", "hyhavenworld.fly", true);
        roleRepository.addPermission("admin", "hyhavenworld.fly", false);

        Set<Permission> permissions = roleRepository.getPermissions("admin");
        assertEquals(1, permissions.size());
        Permission perm = permissions.iterator().next();
        assertEquals("hyhavenworld.fly", perm.getPermissionNode());
        assertFalse(perm.getValue());
    }

    @Test
    @Order(10)
    @DisplayName("Should remove permission from role")
    void testRemovePermission() {
        roleRepository.create(new Role(null, "admin", null, Set.of()));

        roleRepository.addPermission("admin", "hyhavenworld.admin", true);
        roleRepository.removePermission("admin", "hyhavenworld.admin");

        Set<Permission> permissions = roleRepository.getPermissions("admin");
        assertEquals(0, permissions.size());
    }

    @Test
    @Order(11)
    @DisplayName("Should get all permissions for role")
    void testGetPermissions() {
        roleRepository.create(new Role(null, "admin", null, Set.of()));

        roleRepository.addPermission("admin", "hyhavenworld.admin", true);
        roleRepository.addPermission("admin", "hyhavenworld.fly", true);
        roleRepository.addPermission("admin", "hyhavenworld.build", true);

        Set<Permission> permissions = roleRepository.getPermissions("admin");
        assertEquals(3, permissions.size());
    }

    // ===== Role Inheritance Tests =====

    @Test
    @Order(12)
    @DisplayName("Should add inheritance relationship")
    void testAddInheritance() {
        roleRepository.create(new Role(null, "admin", null, Set.of()));
        roleRepository.create(new Role(null, "moderator", null, Set.of()));

        roleRepository.addInheritance("admin", "moderator");

        Set<Role> parents = roleRepository.getParentRoles("moderator");
        assertEquals(1, parents.size());
        assertTrue(parents.stream().anyMatch(r -> r.getName().equals("admin")));
    }

    @Test
    @Order(13)
    @DisplayName("Should remove inheritance relationship")
    void testRemoveInheritance() {
        roleRepository.create(new Role(null, "admin", null, Set.of()));
        roleRepository.create(new Role(null, "moderator", null, Set.of()));

        roleRepository.addInheritance("admin", "moderator");
        roleRepository.removeInheritance("admin", "moderator");

        Set<Role> parents = roleRepository.getParentRoles("moderator");
        assertEquals(0, parents.size());
    }

    @Test
    @Order(14)
    @DisplayName("Should get parent roles")
    void testGetParentRoles() {
        roleRepository.create(new Role(null, "owner", null, Set.of()));
        roleRepository.create(new Role(null, "admin", null, Set.of()));
        roleRepository.create(new Role(null, "moderator", null, Set.of()));

        roleRepository.addInheritance("owner", "admin");
        roleRepository.addInheritance("admin", "moderator");

        Set<Role> parents = roleRepository.getParentRoles("moderator");
        assertEquals(1, parents.size());
        assertTrue(parents.stream().anyMatch(r -> r.getName().equals("admin")));
    }

    @Test
    @Order(15)
    @DisplayName("Should get child roles")
    void testGetChildRoles() {
        roleRepository.create(new Role(null, "admin", null, Set.of()));
        roleRepository.create(new Role(null, "moderator", null, Set.of()));
        roleRepository.create(new Role(null, "helper", null, Set.of()));

        roleRepository.addInheritance("admin", "moderator");
        roleRepository.addInheritance("admin", "helper");

        Set<Role> children = roleRepository.getChildRoles("admin");
        assertEquals(2, children.size());
    }

    // ===== Effective Permissions Tests =====

    @Test
    @Order(16)
    @DisplayName("Should get effective permissions without inheritance")
    void testGetEffectivePermissionsNoInheritance() {
        roleRepository.create(new Role(null, "admin", null, Set.of()));

        roleRepository.addPermission("admin", "hyhavenworld.admin", true);
        roleRepository.addPermission("admin", "hyhavenworld.fly", true);

        Set<Permission> effectivePermissions = roleRepository.getEffectivePermissions("admin");
        assertEquals(2, effectivePermissions.size());
    }

    @Test
    @Order(17)
    @DisplayName("Should get effective permissions with single level inheritance")
    void testGetEffectivePermissionsWithInheritance() {
        roleRepository.create(new Role(null, "admin", null, Set.of()));
        roleRepository.create(new Role(null, "moderator", null, Set.of()));

        roleRepository.addPermission("admin", "hyhavenworld.admin", true);
        roleRepository.addPermission("admin", "hyhavenworld.kick", true);
        roleRepository.addPermission("moderator", "hyhavenworld.mute", true);

        roleRepository.addInheritance("admin", "moderator");

        Set<Permission> effectivePermissions = roleRepository.getEffectivePermissions("moderator");
        assertEquals(3, effectivePermissions.size());
    }

    @Test
    @Order(18)
    @DisplayName("Should get effective permissions with multi-level inheritance")
    void testGetEffectivePermissionsMultiLevel() {
        roleRepository.create(new Role(null, "owner", null, Set.of()));
        roleRepository.create(new Role(null, "admin", null, Set.of()));
        roleRepository.create(new Role(null, "moderator", null, Set.of()));

        roleRepository.addPermission("owner", "hyhavenworld.owner", true);
        roleRepository.addPermission("admin", "hyhavenworld.admin", true);
        roleRepository.addPermission("moderator", "hyhavenworld.mute", true);

        roleRepository.addInheritance("owner", "admin");
        roleRepository.addInheritance("admin", "moderator");

        Set<Permission> effectivePermissions = roleRepository.getEffectivePermissions("moderator");
        assertEquals(3, effectivePermissions.size());
        assertTrue(effectivePermissions.stream().anyMatch(p -> p.getPermissionNode().equals("hyhavenworld.owner")));
        assertTrue(effectivePermissions.stream().anyMatch(p -> p.getPermissionNode().equals("hyhavenworld.admin")));
        assertTrue(effectivePermissions.stream().anyMatch(p -> p.getPermissionNode().equals("hyhavenworld.mute")));
    }

    @Test
    @Order(19)
    @DisplayName("Should handle multiple parent inheritance")
    void testGetEffectivePermissionsMultipleParents() {
        roleRepository.create(new Role(null, "builder", null, Set.of()));
        roleRepository.create(new Role(null, "fighter", null, Set.of()));
        roleRepository.create(new Role(null, "hybrid", null, Set.of()));

        roleRepository.addPermission("builder", "hyhavenworld.build", true);
        roleRepository.addPermission("fighter", "hyhavenworld.pvp", true);
        roleRepository.addPermission("hybrid", "hyhavenworld.trade", true);

        roleRepository.addInheritance("builder", "hybrid");
        roleRepository.addInheritance("fighter", "hybrid");

        Set<Permission> effectivePermissions = roleRepository.getEffectivePermissions("hybrid");
        assertEquals(3, effectivePermissions.size());
    }

    @Test
    @Order(20)
    @DisplayName("Should prevent infinite loops in circular inheritance")
    void testCircularInheritancePrevention() {
        roleRepository.create(new Role(null, "role1", null, Set.of()));
        roleRepository.create(new Role(null, "role2", null, Set.of()));

        roleRepository.addPermission("role1", "perm1", true);
        roleRepository.addPermission("role2", "perm2", true);

        roleRepository.addInheritance("role1", "role2");
        roleRepository.addInheritance("role2", "role1");

        // Should not cause infinite loop
        Set<Permission> effectivePermissions = roleRepository.getEffectivePermissions("role1");
        assertEquals(2, effectivePermissions.size());
    }

    // ===== Error Cases =====

    @Test
    @Order(21)
    @DisplayName("Should throw exception when updating non-existent role")
    void testUpdateNonExistentRole() {
        Role role = new Role(9999L, "nonexistent", null, Set.of());

        assertThrows(CorePersistenceException.class, () -> {
            roleRepository.update(role);
        });
    }

    @Test
    @Order(22)
    @DisplayName("Should throw exception when deleting non-existent role by ID")
    void testDeleteNonExistentRoleById() {
        assertThrows(CorePersistenceException.class, () -> {
            roleRepository.delete(9999L);
        });
    }

    @Test
    @Order(23)
    @DisplayName("Should throw exception when deleting non-existent role by name")
    void testDeleteNonExistentRoleByName() {
        assertThrows(CorePersistenceException.class, () -> {
            roleRepository.deleteByName("nonexistent");
        });
    }

    @Test
    @Order(24)
    @DisplayName("Should return empty set for non-existent role permissions")
    void testGetPermissionsForNonExistentRole() {
        Set<Permission> permissions = roleRepository.getPermissions("nonexistent");
        assertEquals(0, permissions.size());
    }
}