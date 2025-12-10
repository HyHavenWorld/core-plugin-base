package com.hyhavenworld.core.repository.role;

import com.hyhavenworld.core.database.CorePersistenceException;
import com.hyhavenworld.core.database.DatabaseManager;
import com.hyhavenworld.core.domain.Permission;
import com.hyhavenworld.core.domain.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class RoleRepositoryimplJDBC implements RoleRepository{

    private static Logger logger = LoggerFactory.getLogger(RoleRepositoryimplJDBC.class);

    private final DatabaseManager db;

    public RoleRepositoryimplJDBC(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Role createRole(Role role) {
        String sql = """
            INSERT INTO role (name, createdAt)
            VALUES (?, ?)
            RETURNING id
           """;
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            Timestamp ts = new Timestamp(System.currentTimeMillis());
            stmt.setString(1, role.getName());
            stmt.setTimestamp(2, ts);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Long id = (Long) rs.getObject("id");
                    return new Role(id, role.getName(), ts.toLocalDateTime(), Set.of());
                }
                throw new CorePersistenceException("Insert returned no ID");
            }
        } catch (SQLException e) {
            throw new CorePersistenceException("Error inserting role", e);
        }
    }

    @Override
    public Optional<Role> findById(Long id) {
        String sqlRole = "SELECT id, name, created_at FROM role WHERE id = ?";
        String sqlPerms = "SELECT p.id, p.name, p.created_at " +
                "FROM permissions p " +
                "JOIN role_permission rp ON p.id = rp.permission_id " +
                "WHERE rp.role_id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement psRole = conn.prepareStatement(sqlRole)) {

            psRole.setLong(1, id);
            try (ResultSet rsRole = psRole.executeQuery()) {
                if (!rsRole.next()) return Optional.empty();

                String name = rsRole.getString("name");
                LocalDateTime createdAt = rsRole.getTimestamp("created_at").toLocalDateTime();

                // Obtener permisos
                Set<Permission> permissions = new HashSet<>();
                try (PreparedStatement psPerm = conn.prepareStatement(sqlPerms)) {
                    psPerm.setLong(1, id);
                    try (ResultSet rsPerm = psPerm.executeQuery()) {
                        while (rsPerm.next()) {
                            Long pid = rsPerm.getLong("id");
                            String pname = rsPerm.getString("name");
                            LocalDateTime pcreatedAt = rsPerm.getTimestamp("created_at").toLocalDateTime();
                            permissions.add(new Permission(pid, pname, pcreatedAt));
                        }
                    }
                }

                Role role = new Role(id, name, createdAt, permissions);
                return Optional.of(role);
            }
        } catch (SQLException e) {
            throw new CorePersistenceException(e);
        }
    }

    @Override
    public void update(Role role) {
        String sql = "UPDATE role SET name = ? WHERE id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, role.getName());

            stmt.setLong(2, role.getId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new CorePersistenceException("Error updating role with id " + role.getId(), e);
        }
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM role WHERE id = ?";
        Connection conn = null;

        try {
            conn = db.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                logger.error(ex.getMessage(), ex);
            }
            throw new CorePersistenceException("Error deleting role", e);
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
    public Set<Role> findAll() {
        String sqlRoles = "SELECT id, name, created_at FROM role";
        String sqlPerms = "SELECT p.id, p.name, p.created_at " +
                "FROM permission p " +
                "JOIN role_permission rp ON p.id = rp.permission_id " +
                "WHERE rp.role_id = ?";

        Set<Role> roles = new HashSet<>();

        try (Connection conn = db.getConnection();
             PreparedStatement psRoles = conn.prepareStatement(sqlRoles);
             ResultSet rsRoles = psRoles.executeQuery()) {

            while (rsRoles.next()) {
                long roleId = rsRoles.getLong("id");
                String name = rsRoles.getString("name");
                LocalDateTime createdAt = rsRoles.getTimestamp("created_at").toLocalDateTime();

                Set<Permission> permissions = new HashSet<>();
                try (PreparedStatement psPerms = conn.prepareStatement(sqlPerms)) {
                    psPerms.setLong(1, roleId);
                    try (ResultSet rsPerms = psPerms.executeQuery()) {
                        while (rsPerms.next()) {
                            Long permId = rsPerms.getLong("id");
                            String permName = rsPerms.getString("name");
                            LocalDateTime permCreated = rsPerms.getTimestamp("created_at").toLocalDateTime();
                            permissions.add(new Permission(permId, permName, permCreated));
                        }
                    }
                }

                roles.add(new Role(roleId, name, createdAt, permissions));
            }

            return roles;

        } catch (SQLException e) {
            throw new CorePersistenceException("Error fetching roles", e);
        }
    }

    @Override
    public void assignPermission(Long role, Long permission) {
        String sql = "INSERT INTO role_permission(role_id, permission_id) VALUES (?, ?)";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, role);
            stmt.setLong(2, permission);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new CorePersistenceException("Error assigning permission to role", e);
        }
    }

    @Override
    public void unassignPermission(Long role, Long permission) {
        String sql = "DELETE FROM role_permission WHERE role_id = ? AND permission_id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, role);
            ps.setLong(2, permission);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new CorePersistenceException("Error unassigning permission", e);
        }
    }
}
