package com.hyhavenworld.core.repository.user;

import com.hyhavenworld.core.database.CorePersistenceException;
import com.hyhavenworld.core.database.StorageManager;
import com.hyhavenworld.core.domain.Role;
import com.hyhavenworld.core.domain.User;

import java.sql.*;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class UserRepositoryImplJDBC implements UserRepository{

    @Override
    public User create(User user) throws CorePersistenceException {
        String sql = """
        INSERT INTO users (uuid, username, createdAt)
        VALUES (?, ?, ?)
        """;

        try (Connection conn = StorageManager.get().getDatabase().getConnection();
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
    public void addRole(UUID uuid, String role) {

    }

    @Override
    public void removeRole(UUID uuid, String role) {

    }

    @Override
    public Set<Role> getRoles(UUID uuid) {
        return Set.of();
    }

    @Override
    public Optional<User> getUser(UUID uuid) {
        return Optional.empty();
    }

}
