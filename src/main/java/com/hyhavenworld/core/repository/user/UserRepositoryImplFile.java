package com.hyhavenworld.core.repository.user;

import com.hyhavenworld.core.domain.Role;
import com.hyhavenworld.core.domain.User;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class UserRepositoryImplFile implements UserRepository {

    public UserRepositoryImplFile(String filePath) {

    }

    @Override
    public User create(User user) {
        return null;
    }

    @Override
    public Optional<User> getUser(UUID uuid) {
        return Optional.empty();
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
}
