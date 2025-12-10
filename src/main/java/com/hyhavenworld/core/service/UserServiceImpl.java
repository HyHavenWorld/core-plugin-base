package com.hyhavenworld.core.service;

import com.hyhavenworld.core.api.UserService;
import com.hyhavenworld.core.domain.Role;
import com.hyhavenworld.core.domain.User;
import com.hyhavenworld.core.repository.user.UserRepository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class UserServiceImpl implements UserService {

    private UserRepository userRepository;

    @Override
    public Optional<User> getUser(UUID playerId) {
        return this.userRepository.getUser(playerId);
    }

    @Override
    public void addRole(UUID playerId, String roleKey) {
        this.userRepository.addRole(playerId, roleKey);
    }

    @Override
    public void removeRole(UUID playerId, String roleKey) {
        this.userRepository.removeRole(playerId, roleKey);
    }

    @Override
    public Set<Role> getRoles(UUID playerId) {
        return Set.of();
    }
}

