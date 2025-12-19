package com.hyhavenworld.core.service;

import com.hyhavenworld.core.api.RoleService;
import com.hyhavenworld.core.cache.CacheManager;
import com.hyhavenworld.core.config.CoreConfig;
import com.hyhavenworld.core.domain.Role;
import com.hyhavenworld.core.repository.role.RoleRepository;
import com.hyhavenworld.core.repository.role.RoleRepositoryImplFile;
import com.hyhavenworld.core.repository.role.RoleRepositoryImplJDBC;

import java.util.Optional;

public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final CacheManager cacheManager;

    public RoleServiceImpl(CoreConfig coreConfig) {
        this.roleRepository = switch(coreConfig.getStorageType()) {
            case DATABASE ->  new RoleRepositoryImplJDBC();
            case FILE -> new RoleRepositoryImplFile(coreConfig.getFilePath());
            default -> new RoleRepositoryImplFile(coreConfig.getFilePath());
        };
        this.cacheManager = new CacheManager(coreConfig.getCacheConfig());
    }

    @Override
    public Optional<Role> getRole(Long id) {
        // Note: We cache by name, not ID, so we can't cache this lookup efficiently
        // This is less commonly used than getByName anyway
        return roleRepository.getById(id);
    }

    @Override
    public void createRole(Role role) {
        roleRepository.create(role);
        // No need to cache on create - will be cached on first read
    }

    @Override
    public void addInheritance(String parent, String child) {
        roleRepository.addInheritance(parent, child);
        // Invalidate both roles since hierarchy changed
        cacheManager.invalidateRole(parent);
        cacheManager.invalidateRole(child);
    }

    /**
     * Get role by name with caching support.
     * This is a commonly used operation, so it benefits from caching.
     */
    public Optional<Role> getRoleByName(String name) {
        // Try cache first
        Role cached = cacheManager.getRoleFromCache(name);
        if (cached != null) {
            return Optional.of(cached);
        }

        // Cache miss - load from repository
        Optional<Role> role = roleRepository.getByName(name);

        // Update cache
        role.ifPresent(r -> cacheManager.cacheRole(name, r));

        return role;
    }

    /**
     * Add permission to role and invalidate cache.
     */
    public void addPermission(String roleName, String permissionNode, boolean value) {
        roleRepository.addPermission(roleName, permissionNode, value);
        // Invalidate role cache since permissions changed
        cacheManager.invalidateRole(roleName);
    }

    /**
     * Get cache manager for monitoring purposes.
     * Package-private for internal use.
     */
    CacheManager getCacheManager() {
        return cacheManager;
    }
}
