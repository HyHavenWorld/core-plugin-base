package com.hyhavenworld.core.service;

import com.hyhavenworld.core.api.UserService;
import com.hyhavenworld.core.config.CoreConfig;
import com.hyhavenworld.core.domain.Role;
import com.hyhavenworld.core.domain.User;
import com.hyhavenworld.core.repository.user.UserRepository;
import com.hyhavenworld.core.repository.user.UserRepositoryImplFile;
import com.hyhavenworld.core.repository.user.UserRepositoryImplJDBC;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class UserServiceImpl implements UserService {

    private UserRepository userRepository;

    public UserServiceImpl( CoreConfig coreConfig ) {
        this.userRepository = switch(coreConfig.getStorageType()) {
            case DATABASE ->  new UserRepositoryImplJDBC();
            case FILE -> new UserRepositoryImplFile(coreConfig.getFilePath());
            default -> new UserRepositoryImplFile(coreConfig.getFilePath());
        };
    }

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
        return this.userRepository.getRoles(playerId);
    }
}

