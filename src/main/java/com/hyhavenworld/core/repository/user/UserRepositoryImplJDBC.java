package com.hyhavenworld.core.repository.user;

import com.hyhavenworld.core.database.CorePersistenceException;
import com.hyhavenworld.core.database.StorageManager;
import com.hyhavenworld.core.domain.Permission;
import com.hyhavenworld.core.domain.Role;
import com.hyhavenworld.core.domain.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class UserRepositoryImplJDBC implements UserRepository {

    // ===== CRUD básico de usuarios =====

    @Override
    public User create(User user) throws CorePersistenceException {
        String sql = """
        INSERT INTO users (uuid, username, created_at, last_seen, hours_played)
        VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection conn = StorageManager.get().getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            Timestamp now = new Timestamp(System.currentTimeMillis());

            stmt.setString(1, user.getUuid());
            stmt.setString(2, user.getUsername());
            stmt.setTimestamp(3, now);
            stmt.setTimestamp(4, now);
            stmt.setLong(5, 0L);

            int affected = stmt.executeUpdate();

            if (affected == 0) {
                throw new CorePersistenceException("Insert returned 0 affected rows");
            }

            return new User(
                user.getUuid(),
                user.getUsername(),
                now.toLocalDateTime(),
                now.toLocalDateTime(),
                0L,
                Set.of(),
                Set.of()
            );

        } catch (SQLException e) {
            throw new CorePersistenceException("Error inserting user", e);
        }
    }

    @Override
    public Optional<User> getUser(UUID uuid) throws CorePersistenceException {
        String sql = """
        SELECT uuid, username, created_at, last_seen, hours_played
        FROM users
        WHERE uuid = ?
        """;

        try (Connection conn = StorageManager.get().getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User(
                        rs.getString("uuid"),
                        rs.getString("username"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("last_seen").toLocalDateTime(),
                        rs.getLong("hours_played"),
                        getRoles(uuid),
                        getPermissions(uuid)
                    );
                    return Optional.of(user);
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new CorePersistenceException("Error getting user by UUID", e);
        }
    }

    @Override
    public Optional<User> getUserByUsername(String username) throws CorePersistenceException {
        String sql = """
        SELECT uuid, username, created_at, last_seen, hours_played
        FROM users
        WHERE username = ?
        """;

        try (Connection conn = StorageManager.get().getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    User user = new User(
                        rs.getString("uuid"),
                        rs.getString("username"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("last_seen").toLocalDateTime(),
                        rs.getLong("hours_played"),
                        getRoles(uuid),
                        getPermissions(uuid)
                    );
                    return Optional.of(user);
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new CorePersistenceException("Error getting user by username", e);
        }
    }

    @Override
    public void update(User user) throws CorePersistenceException {
        String sql = """
        UPDATE users
        SET username = ?, last_seen = ?, hours_played = ?
        WHERE uuid = ?
        """;

        try (Connection conn = StorageManager.get().getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getUsername());
            stmt.setTimestamp(2, Timestamp.valueOf(user.getLastSeen()));
            stmt.setLong(3, user.getHoursPlayed());
            stmt.setString(4, user.getUuid());

            int affected = stmt.executeUpdate();

            if (affected == 0) {
                throw new CorePersistenceException("Update returned 0 affected rows");
            }

        } catch (SQLException e) {
            throw new CorePersistenceException("Error updating user", e);
        }
    }

    @Override
    public void delete(UUID uuid) throws CorePersistenceException {
        String sql = "DELETE FROM users WHERE uuid = ?";

        try (Connection conn = StorageManager.get().getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());

            int affected = stmt.executeUpdate();

            if (affected == 0) {
                throw new CorePersistenceException("Delete returned 0 affected rows");
            }

        } catch (SQLException e) {
            throw new CorePersistenceException("Error deleting user", e);
        }
    }

    @Override
    public Set<User> findAll() throws CorePersistenceException {
        String sql = "SELECT uuid, username, created_at, last_seen, hours_played FROM users";
        Set<User> users = new HashSet<>();

        try (Connection conn = StorageManager.get().getDatabase().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                User user = new User(
                    rs.getString("uuid"),
                    rs.getString("username"),
                    rs.getTimestamp("created_at").toLocalDateTime(),
                    rs.getTimestamp("last_seen").toLocalDateTime(),
                    rs.getLong("hours_played"),
                    getRoles(uuid),
                    getPermissions(uuid)
                );
                users.add(user);
            }

            return users;

        } catch (SQLException e) {
            throw new CorePersistenceException("Error getting all users", e);
        }
    }

    // ===== Gestión de roles (tabla user_roles) =====

    @Override
    public void addRole(UUID uuid, String roleName) throws CorePersistenceException {
        try (Connection conn = StorageManager.get().getDatabase().getConnection()) {

            // Check if already exists
            String checkSql = """
            SELECT COUNT(*)
            FROM user_roles ur
            INNER JOIN roles r ON ur.role_id = r.id
            WHERE ur.user_uuid = ? AND r.name = ?
            """;

            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, uuid.toString());
                checkStmt.setString(2, roleName);

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return; // Already exists, nothing to do
                    }
                }
            }

            // Insert if doesn't exist
            String insertSql = """
            INSERT INTO user_roles (user_uuid, role_id)
            SELECT ?, id FROM roles WHERE name = ?
            """;

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, uuid.toString());
                insertStmt.setString(2, roleName);
                insertStmt.executeUpdate();
            }

        } catch (SQLException e) {
            throw new CorePersistenceException("Error adding role to user", e);
        }
    }

    @Override
    public void removeRole(UUID uuid, String roleName) throws CorePersistenceException {
        String sql = """
        DELETE FROM user_roles
        WHERE user_uuid = ? AND role_id = (SELECT id FROM roles WHERE name = ?)
        """;

        try (Connection conn = StorageManager.get().getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());
            stmt.setString(2, roleName);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new CorePersistenceException("Error removing role from user", e);
        }
    }

    @Override
    public Set<Role> getRoles(UUID uuid) throws CorePersistenceException {
        String sql = """
        SELECT r.id, r.name, r.created_at
        FROM roles r
        INNER JOIN user_roles ur ON r.id = ur.role_id
        WHERE ur.user_uuid = ?
        """;

        Set<Role> roles = new HashSet<>();

        try (Connection conn = StorageManager.get().getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Role role = new Role(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        Set.of() // Permissions will be loaded separately if needed
                    );
                    roles.add(role);
                }
            }

            return roles;

        } catch (SQLException e) {
            throw new CorePersistenceException("Error getting user roles", e);
        }
    }

    // ===== Gestión de permisos directos (tabla user_permissions) =====

    @Override
    public void addPermission(UUID uuid, String permissionNode, boolean value) throws CorePersistenceException {
        try (Connection conn = StorageManager.get().getDatabase().getConnection()) {

            // Try UPDATE first
            String updateSql = """
            UPDATE user_permissions
            SET perm_value = ?
            WHERE user_uuid = ? AND permission_node = ?
            """;

            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setBoolean(1, value);
                stmt.setString(2, uuid.toString());
                stmt.setString(3, permissionNode);

                int affected = stmt.executeUpdate();

                if (affected == 0) {
                    // If UPDATE didn't affect any rows, do INSERT
                    String insertSql = """
                    INSERT INTO user_permissions (user_uuid, permission_node, perm_value)
                    VALUES (?, ?, ?)
                    """;

                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setString(1, uuid.toString());
                        insertStmt.setString(2, permissionNode);
                        insertStmt.setBoolean(3, value);
                        insertStmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            throw new CorePersistenceException("Error adding permission to user", e);
        }
    }

    @Override
    public void removePermission(UUID uuid, String permissionNode) throws CorePersistenceException {
        String sql = "DELETE FROM user_permissions WHERE user_uuid = ? AND permission_node = ?";

        try (Connection conn = StorageManager.get().getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());
            stmt.setString(2, permissionNode);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new CorePersistenceException("Error removing permission from user", e);
        }
    }

    @Override
    public Set<Permission> getPermissions(UUID uuid) throws CorePersistenceException {
        String sql = """
        SELECT permission_node, perm_value
        FROM user_permissions
        WHERE user_uuid = ?
        """;

        Set<Permission> permissions = new HashSet<>();

        try (Connection conn = StorageManager.get().getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Permission permission = new Permission(
                        rs.getString("permission_node"),
                        rs.getBoolean("perm_value")
                    );
                    permissions.add(permission);
                }
            }

            return permissions;

        } catch (SQLException e) {
            throw new CorePersistenceException("Error getting user permissions", e);
        }
    }

    // ===== Métodos de actualización de actividad =====

    @Override
    public void updateLastSeen(UUID uuid, LocalDateTime timestamp) throws CorePersistenceException {
        String sql = "UPDATE users SET last_seen = ? WHERE uuid = ?";

        try (Connection conn = StorageManager.get().getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(timestamp));
            stmt.setString(2, uuid.toString());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new CorePersistenceException("Error updating last_seen", e);
        }
    }

    @Override
    public void updateHoursPlayed(UUID uuid, long hours) throws CorePersistenceException {
        String sql = "UPDATE users SET hours_played = ? WHERE uuid = ?";

        try (Connection conn = StorageManager.get().getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, hours);
            stmt.setString(2, uuid.toString());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new CorePersistenceException("Error updating hours_played", e);
        }
    }
}