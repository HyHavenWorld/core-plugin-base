package com.hyhavenworld.core.repository.permission;

import com.hyhavenworld.core.domain.Permission;

import java.util.Optional;
import java.util.Set;

public interface PermissionRepository {
    Permission create(Permission permission);
    Optional<Permission> findById(Long id);
    void update(Permission permission);
    void delete(Long id);
    Set<Permission> findAll();
}
