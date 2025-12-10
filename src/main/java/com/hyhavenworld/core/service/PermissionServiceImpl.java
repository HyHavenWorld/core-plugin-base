package com.hyhavenworld.core.service;

import com.hyhavenworld.core.api.PermissionService;

import java.util.UUID;

public class PermissionServiceImpl implements PermissionService {
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
