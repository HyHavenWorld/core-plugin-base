package com.hyhavenworld.core.repository.user;

import com.hyhavenworld.core.domain.Role;
import com.hyhavenworld.core.domain.User;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UserRepository {

    User create(User user);
    Optional<User> getUser(UUID uuid);
    void addRole(UUID uuid, String role);
    void removeRole(UUID uuid, String role);
    Set<Role> getRoles(UUID uuid);
}
