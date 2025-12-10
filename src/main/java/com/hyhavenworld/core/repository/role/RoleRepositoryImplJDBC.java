package com.hyhavenworld.core.repository.role;

import com.hyhavenworld.core.database.CorePersistenceException;
import com.hyhavenworld.core.database.StorageManager;
import com.hyhavenworld.core.domain.Role;

import java.sql.*;
import java.util.Optional;
import java.util.Set;

public class RoleRepositoryImplJDBC implements RoleRepository{

    @Override
    public Role createRole(Role role) {
        String sql = """
            INSERT INTO role (name, createdAt)
            VALUES (?, ?)
            RETURNING id
           """;
        try (Connection conn = StorageManager.get().getDatabase().getConnection();
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
    public Optional<Role> getRole(int id) {
        return Optional.empty();
    }

    @Override
    public void addInheritance(String parent, String child) {

    }

}
