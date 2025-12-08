package com.hyhavenworld.core.repository.user;

import com.hyhavenworld.core.database.CorePersistenceException;
import com.hyhavenworld.core.database.DatabaseManager;
import com.hyhavenworld.core.domain.Role;
import com.hyhavenworld.core.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class UserRepositoryImplJDBC implements UserRepository{

    private static Logger logger = LoggerFactory.getLogger(UserRepositoryImplJDBC.class);

    private final DatabaseManager db;

    public UserRepositoryImplJDBC(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public User create(User user) throws CorePersistenceException {
        String sql = """
        INSERT INTO users (uuid, username, createdAt)
        VALUES (?, ?, ?)
        """;

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            Timestamp now = new Timestamp(System.currentTimeMillis());

            stmt.setString(1, user.getUuid());
            stmt.setString(2, user.getUsername());
            stmt.setTimestamp(3, now);

            int affected = stmt.executeUpdate();

            if (affected == 0) {
                throw new CorePersistenceException("Insert returned 0 affected rows");
            }

            return new User(user.getUuid(), user.getUsername(), now.toLocalDateTime(), now.toLocalDateTime(), 0l, Set.of());

        } catch (SQLException e) {
            throw new CorePersistenceException("Error inserting user", e);
        }
    }

    @Override
    public Optional<User> findById(String uuid) {
        String sqlUser = "SELECT uuid, username, created_at, last_seen, hours_played FROM user WHERE id = ?";
        String sqlRole = "SELECT r.id, r.name, p.created_at " +
                "FROM role r " +
                "JOIN user_role ur ON r.id = ur.role_id " +
                "WHERE ur.user_id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement psUser = conn.prepareStatement(sqlUser)) {

            psUser.setString(1, uuid);
            try (ResultSet rsUser = psUser.executeQuery()) {
                if (!rsUser.next()) return Optional.empty();

                String name = rsUser.getString("name");
                LocalDateTime createdAt = rsUser.getTimestamp("created_at").toLocalDateTime();
                LocalDateTime lastSeen = rsUser.getTimestamp("last_seen").toLocalDateTime();
                Long hoursPlayed = rsUser.getLong("hours_played");

                // Obtener roles
                Set<Role> roles = new HashSet<>();
                try (PreparedStatement psRole = conn.prepareStatement(sqlRole)) {
                    psRole.setString(1, uuid);
                    try (ResultSet rsrole = psRole.executeQuery()) {
                        while (rsrole.next()) {
                            Long pid = rsrole.getLong("id");
                            String pname = rsrole.getString("name");
                            LocalDateTime pcreatedAt = rsrole.getTimestamp("created_at").toLocalDateTime();
                            roles.add(new Role(pid, pname, pcreatedAt, Set.of())); // Lazy load -> no cargamos los permisos en cada peticion de user
                        }
                    }
                }

                User user = new User(uuid, name, createdAt, lastSeen, hoursPlayed, roles);
                return Optional.of(user);
            }
        } catch (SQLException e) {
            throw new CorePersistenceException(e);
        }
    }

    @Override
    public void update(User user) {
        String sql = """
        UPDATE users
        SET username = ?, lastSeen = ?, hoursPlayed = ?, updated_at = ?
        WHERE id = ?
        """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, user.getUsername());
                ps.setTimestamp(2, Timestamp.valueOf(user.getLastSeen()));
                ps.setLong(3, user.getHoursPlayed());
                ps.setString(5, user.getUuid());
                ps.executeUpdate();

        } catch (SQLException e) {
            throw new CorePersistenceException("Error updating user", e);
        }
    }

    @Override
    public void delete(String uuid) {
        String sql = "DELETE FROM users WHERE id = ?"; // cascade borrará role_permissions
        Connection conn = null;

        try {
            conn = db.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid);
                ps.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                logger.error(ex.getMessage(), ex);
            }
            throw new CorePersistenceException("Error deleting user", e);
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }

    @Override
    public void assignRole(String uuid, Long role) throws CorePersistenceException {
        String sql = "INSERT INTO user_role(user_id, role_id) VALUES (?, ?)";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, uuid);
            stmt.setLong(2, role);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new CorePersistenceException("Error assigning role", e);
        }
    }

    @Override
    public void unassignRole(String uuid, Long role) {
        String sql = "DELETE FROM user_role WHERE user_id = ? AND role_id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uuid);
            ps.setLong(2, role);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new CorePersistenceException("Error unassigning role", e);
        }
    }
}
