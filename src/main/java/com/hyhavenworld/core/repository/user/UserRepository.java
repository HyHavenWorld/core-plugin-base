package com.hyhavenworld.core.repository.user;

import com.hyhavenworld.core.domain.User;

import java.util.Optional;

public interface UserRepository {

    User create(User user);
    Optional<User> findById(String uuid);
    void update(User user);
    void delete(String uuid);
    void assignRole(String uuid, Long role);
    void unassignRole(String uuid, Long role);
}
