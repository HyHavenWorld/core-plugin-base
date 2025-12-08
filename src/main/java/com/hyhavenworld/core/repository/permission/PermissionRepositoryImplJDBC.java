package com.hyhavenworld.core.repository.permission;

import com.hyhavenworld.core.database.CorePersistenceException;
import com.hyhavenworld.core.database.DatabaseManager;
import com.hyhavenworld.core.domain.Permission;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class PermissionRepositoryImplJDBC implements PermissionRepository {

    private final DatabaseManager db;

    public PermissionRepositoryImplJDBC(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Permission create(Permission permission) {
        String sql = """
            INSERT INTO permission (name, createdAt)
            VALUES (?, ?)
            RETURNING id
           """;
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            Timestamp ts = new Timestamp(System.currentTimeMillis());
            stmt.setString(1, permission.getName());
            stmt.setTimestamp(2, ts);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Long id = (Long) rs.getObject("id");
                    return new Permission(id, permission.getName(), ts.toLocalDateTime());
                }
                throw new CorePersistenceException("Insert returned no ID");
            }
        } catch (SQLException e) {
            throw new CorePersistenceException("Error inserting permission", e);
        }
    }

    @Override
    public Optional<Permission> findById(Long id) {
        String sql = "SELECT id, name, created_at FROM permissions WHERE id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, id); // UUID OK

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                Permission perm = new Permission(
                        (Long) rs.getObject("id"),
                        rs.getString("name"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                );

                return Optional.of(perm);
            }

        } catch (SQLException e) {
            throw new CorePersistenceException("Error finding permission by id", e);
        }
    }

    @Override
    public void update(Permission permission) {
        String sql = "UPDATE permissions SET name = ? WHERE id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, permission.getName());

            stmt.setLong(2, permission.getId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new CorePersistenceException("Error updating permission with id " + permission.getId(), e);
        }
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM permissions WHERE id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, id);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new CorePersistenceException("Error deleting permission with id: " + id, e);
        }
    }

    @Override
    public Set<Permission> findAll() {
        Set<Permission> permissions = new HashSet<>();
        String sql = "SELECT id, name, created_at FROM permissions";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Long id = rs.getLong("id");
                String name = rs.getString("name");
                LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();

                Permission p = new Permission(id, name, createdAt);
                permissions.add(p);
            }
        } catch (SQLException e) {
            throw new CorePersistenceException("Error finding permissions", e);
        }
        return permissions;
    }
}
