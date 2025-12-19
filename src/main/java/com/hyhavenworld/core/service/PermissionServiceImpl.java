package com.hyhavenworld.core.service;

import com.hyhavenworld.core.api.PermissionService;
import com.hyhavenworld.core.config.CoreConfig;
import com.hyhavenworld.core.domain.Permission;
import com.hyhavenworld.core.domain.Role;
import com.hyhavenworld.core.repository.role.RoleRepository;
import com.hyhavenworld.core.repository.role.RoleRepositoryImplFile;
import com.hyhavenworld.core.repository.role.RoleRepositoryImplJDBC;
import com.hyhavenworld.core.repository.user.UserRepository;
import com.hyhavenworld.core.repository.user.UserRepositoryImplFile;
import com.hyhavenworld.core.repository.user.UserRepositoryImplJDBC;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class PermissionServiceImpl implements PermissionService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public PermissionServiceImpl(CoreConfig coreConfig) {
        this.userRepository = switch (coreConfig.getStorageType()) {
            case DATABASE -> new UserRepositoryImplJDBC();
            case FILE -> new UserRepositoryImplFile(coreConfig.getFilePath());
        };
        this.roleRepository = switch (coreConfig.getStorageType()) {
            case DATABASE -> new RoleRepositoryImplJDBC();
            case FILE -> new RoleRepositoryImplFile(coreConfig.getFilePath());
        };
    }

    @Override
    public boolean hasPermission(UUID playerId, String permissionKey) {
        // Permission Hierarchy:
        // 1. Check user direct permissions (highest priority)
        Set<Permission> userPermissions = userRepository.getPermissions(playerId);
        Optional<Permission> userPermission = userPermissions.stream()
            .filter(p -> p.getPermissionNode().equals(permissionKey))
            .findFirst();

        if (userPermission.isPresent()) {
            return userPermission.get().getValue();
        }

        // 2. Check permissions in user's roles
        Set<Role> userRoles = userRepository.getRoles(playerId);
        Set<Permission> allRolePermissions = new HashSet<>();

        for (Role role : userRoles) {
            // Get effective permissions (includes inherited permissions)
            Set<Permission> rolePerms = roleRepository.getEffectivePermissions(role.getName());
            allRolePermissions.addAll(rolePerms);
        }

        Optional<Permission> rolePermission = allRolePermissions.stream()
            .filter(p -> p.getPermissionNode().equals(permissionKey))
            .findFirst();

        if (rolePermission.isPresent()) {
            return rolePermission.get().getValue();
        }

        // 3. Default: no permission
        return false;
    }
}
