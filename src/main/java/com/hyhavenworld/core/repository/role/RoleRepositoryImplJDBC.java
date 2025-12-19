package com.hyhavenworld.core.repository.role;

import com.hyhavenworld.core.database.CorePersistenceException;
import com.hyhavenworld.core.database.StorageManager;
import com.hyhavenworld.core.domain.Permission;
import com.hyhavenworld.core.domain.Role;

import java.sql.*;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class RoleRepositoryImplJDBC implements RoleRepository {

    // ===== CRUD básico de roles =====

    @Override
    public Role create(Role role) throws CorePersistenceException {
        String sql = """
            INSERT INTO roles (name, created_at)
            VALUES (?, ?)
            RETURNING id, created_at
           """;

        try (Connection conn = StorageManager.get().getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            Timestamp ts = new Timestamp(System.currentTimeMillis());
            stmt.setString(1, role.getName());
            stmt.setTimestamp(2, ts);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong("id");
                    return new Role(
                        id,
                        role.getName(),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        Set.of()
                    );
                }
                throw new CorePersistenceException("Insert returned no ID");
            }
        } catch (SQLException e) {
            throw new CorePersistenceException("Error inserting role", e);
        }
    }

    @Override
    public Optional<Role> getById(Long id) throws CorePersistenceException {
        String sql = "SELECT id, name, created_at FROM roles WHERE id = ?";

        try (Connection conn = StorageManager.get().getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Role role = new Role(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        getPermissions(rs.getString("name"))
                    );
                    return Optional.of(role);
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new CorePersistenceException("Error getting role by ID", e);
        }
    }

    @Override
    public Optional<Role> getByName(String name) throws CorePersistenceException {
        String sql = "SELECT id, name, created_at FROM roles WHERE name = ?";

        try (Connection conn = StorageManager.get().getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Role role = new Role(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        getPermissions(name)
                    );
                    return Optional.of(role);
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new CorePersistenceException("Error getting role by name", e);
        }
    }

    @Override
    public void update(Role role) throws CorePersistenceException {
        String sql = "UPDATE roles SET name = ? WHERE id = ?";

        try (Connection conn = StorageManager.get().getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, role.getName());
            stmt.setLong(2, role.getId());

            int affected = stmt.executeUpdate();

            if (affected == 0) {
                throw new CorePersistenceException("Update returned 0 affected rows");
            }

        } catch (SQLException e) {
            throw new CorePersistenceException("Error updating role", e);
        }
    }

    @Override
    public void delete(Long id) throws CorePersistenceException {
        String sql = "DELETE FROM roles WHERE id = ?";

        try (Connection conn = StorageManager.get().getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            int affected = stmt.executeUpdate();

            if (affected == 0) {
                throw new CorePersistenceException("Delete returned 0 affected rows");
            }

        } catch (SQLException e) {
            throw new CorePersistenceException("Error deleting role", e);
        }
    }

    @Override
    public void deleteByName(String name) throws CorePersistenceException {
        String sql = "DELETE FROM roles WHERE name = ?";

        try (Connection conn = StorageManager.get().getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);

            int affected = stmt.executeUpdate();

            if (affected == 0) {
                throw new CorePersistenceException("Delete returned 0 affected rows");
            }

        } catch (SQLException e) {
            throw new CorePersistenceException("Error deleting role by name", e);
        }
    }

    @Override
    public Set<Role> findAll() throws CorePersistenceException {
        String sql = "SELECT id, name, created_at FROM roles";
        Set<Role> roles = new HashSet<>();

        try (Connection conn = StorageManager.get().getDatabase().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Role role = new Role(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getTimestamp("created_at").toLocalDateTime(),
                    getPermissions(rs.getString("name"))
                );
                roles.add(role);
            }

            return roles;

        } catch (SQLException e) {
            throw new CorePersistenceException("Error getting all roles", e);
        }
    }

    // ===== Gestión de permisos de roles (tabla role_permissions) =====

    @Override
    public void addPermission(String roleName, String permissionNode, boolean value) throws CorePersistenceException {
        String sql = """
        INSERT INTO role_permissions (role_id, permission_node, value)
        SELECT id, ?, ? FROM roles WHERE name = ?
        ON CONFLICT (role_id, permission_node)
        DO UPDATE SET value = EXCLUDED.value
        """;

        try (Connection conn = StorageManager.get().getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, permissionNode);
            stmt.setBoolean(2, value);
            stmt.setString(3, roleName);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new CorePersistenceException("Error adding permission to role", e);
        }
    }

    @Override
    public void removePermission(String roleName, String permissionNode) throws CorePersistenceException {
        String sql = """
        DELETE FROM role_permissions
        WHERE role_id = (SELECT id FROM roles WHERE name = ?) AND permission_node = ?
        """;

        try (Connection conn = StorageManager.get().getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roleName);
            stmt.setString(2, permissionNode);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new CorePersistenceException("Error removing permission from role", e);
        }
    }

    @Override
    public Set<Permission> getPermissions(String roleName) throws CorePersistenceException {
        String sql = """
        SELECT rp.permission_node, rp.value
        FROM role_permissions rp
        INNER JOIN roles r ON rp.role_id = r.id
        WHERE r.name = ?
        """;

        Set<Permission> permissions = new HashSet<>();

        try (Connection conn = StorageManager.get().getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roleName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Permission permission = new Permission(
                        rs.getString("permission_node"),
                        rs.getBoolean("value")
                    );
                    permissions.add(permission);
                }
            }

            return permissions;

        } catch (SQLException e) {
            throw new CorePersistenceException("Error getting role permissions", e);
        }
    }

    // ===== Gestión de herencia de roles (tabla role_inheritance) =====

    @Override
    public void addInheritance(String parentRoleName, String childRoleName) throws CorePersistenceException {
        String sql = """
        INSERT INTO role_inheritance (parent_role_id, child_role_id)
        SELECT p.id, c.id
        FROM roles p, roles c
        WHERE p.name = ? AND c.name = ?
        ON CONFLICT DO NOTHING
        """;

        try (Connection conn = StorageManager.get().getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, parentRoleName);
            stmt.setString(2, childRoleName);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new CorePersistenceException("Error adding role inheritance", e);
        }
    }

    @Override
    public void removeInheritance(String parentRoleName, String childRoleName) throws CorePersistenceException {
        String sql = """
        DELETE FROM role_inheritance
        WHERE parent_role_id = (SELECT id FROM roles WHERE name = ?)
          AND child_role_id = (SELECT id FROM roles WHERE name = ?)
        """;

        try (Connection conn = StorageManager.get().getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, parentRoleName);
            stmt.setString(2, childRoleName);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new CorePersistenceException("Error removing role inheritance", e);
        }
    }

    @Override
    public Set<Role> getParentRoles(String roleName) throws CorePersistenceException {
        String sql = """
        SELECT r.id, r.name, r.created_at
        FROM roles r
        INNER JOIN role_inheritance ri ON r.id = ri.parent_role_id
        INNER JOIN roles child ON child.id = ri.child_role_id
        WHERE child.name = ?
        """;

        Set<Role> parentRoles = new HashSet<>();

        try (Connection conn = StorageManager.get().getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roleName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Role role = new Role(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        Set.of() // Avoid circular loading
                    );
                    parentRoles.add(role);
                }
            }

            return parentRoles;

        } catch (SQLException e) {
            throw new CorePersistenceException("Error getting parent roles", e);
        }
    }

    @Override
    public Set<Role> getChildRoles(String roleName) throws CorePersistenceException {
        String sql = """
        SELECT r.id, r.name, r.created_at
        FROM roles r
        INNER JOIN role_inheritance ri ON r.id = ri.child_role_id
        INNER JOIN roles parent ON parent.id = ri.parent_role_id
        WHERE parent.name = ?
        """;

        Set<Role> childRoles = new HashSet<>();

        try (Connection conn = StorageManager.get().getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roleName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Role role = new Role(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        Set.of() // Avoid circular loading
                    );
                    childRoles.add(role);
                }
            }

            return childRoles;

        } catch (SQLException e) {
            throw new CorePersistenceException("Error getting child roles", e);
        }
    }

    @Override
    public Set<Permission> getEffectivePermissions(String roleName) throws CorePersistenceException {
        Set<Permission> effectivePermissions = new HashSet<>();
        Set<String> visited = new HashSet<>();
        collectPermissionsRecursively(roleName, effectivePermissions, visited);
        return effectivePermissions;
    }

    /**
     * Recursively collects permissions from a role and its parent roles
     */
    private void collectPermissionsRecursively(String roleName, Set<Permission> accumulated, Set<String> visited)
            throws CorePersistenceException {

        // Prevent infinite loops
        if (visited.contains(roleName)) {
            return;
        }
        visited.add(roleName);

        // Add direct permissions from this role
        Set<Permission> directPermissions = getPermissions(roleName);
        accumulated.addAll(directPermissions);

        // Recursively add permissions from parent roles
        Set<Role> parents = getParentRoles(roleName);
        for (Role parent : parents) {
            collectPermissionsRecursively(parent.getName(), accumulated, visited);
        }
    }
}