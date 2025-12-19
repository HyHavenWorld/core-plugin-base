package com.hyhavenworld.core.service;

import com.hyhavenworld.core.api.UserService;
import com.hyhavenworld.core.cache.CacheManager;
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

    private final UserRepository userRepository;
    private final CacheManager cacheManager;

    public UserServiceImpl(CoreConfig coreConfig) {
        this.userRepository = switch(coreConfig.getStorageType()) {
            case DATABASE ->  new UserRepositoryImplJDBC();
            case FILE -> new UserRepositoryImplFile(coreConfig.getFilePath());
            default -> new UserRepositoryImplFile(coreConfig.getFilePath());
        };
        this.cacheManager = new CacheManager(coreConfig.getCacheConfig());
    }

    @Override
    public Optional<User> getUser(UUID playerId) {
        // Try cache first
        User cached = cacheManager.getUserFromCache(playerId);
        if (cached != null) {
            return Optional.of(cached);
        }

        // Cache miss - load from repository
        Optional<User> user = userRepository.getUser(playerId);

        // Update cache
        user.ifPresent(u -> cacheManager.cacheUser(playerId, u));

        return user;
    }

    @Override
    public void addRole(UUID playerId, String roleKey) {
        userRepository.addRole(playerId, roleKey);
        // Invalidate user cache since roles changed
        cacheManager.invalidateUser(playerId);
    }

    @Override
    public void removeRole(UUID playerId, String roleKey) {
        userRepository.removeRole(playerId, roleKey);
        // Invalidate user cache since roles changed
        cacheManager.invalidateUser(playerId);
    }

    @Override
    public Set<Role> getRoles(UUID playerId) {
        // Note: We don't cache this separately as it's part of the User object
        // If we cached the user, roles are already included
        return userRepository.getRoles(playerId);
    }

    /**
     * Get cache manager for monitoring purposes.
     * Package-private for internal use.
     */
    CacheManager getCacheManager() {
        return cacheManager;
    }
}

