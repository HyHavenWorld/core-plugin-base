package com.hyhavenworld.core.service;

import com.hyhavenworld.core.api.PermissionService;
import com.hyhavenworld.core.repository.permission.PermissionRepository;
import com.hyhavenworld.core.repository.user.UserRepository;

import java.util.UUID;

public class PermissionServiceImpl implements PermissionService {

    private UserRepository userRepository;
    private PermissionRepository permissionRepository;

    public PermissionServiceImpl() {}
    
    @Override
    public boolean hasPermission(UUID playerId, String permissionKey) {
        // Permission Hierarchy
        // 1. Check user permission

        // 2. Check permission in roles for the user

        // 3. Check permission in parent roles for the user roles

        // user permission > role_user permission > role_parent permission > false
        return false;
    }
}
