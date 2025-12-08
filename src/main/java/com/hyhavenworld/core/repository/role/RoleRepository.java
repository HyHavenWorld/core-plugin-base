package com.hyhavenworld.core.repository.role;

import com.hyhavenworld.core.domain.Role;

import java.util.Optional;
import java.util.Set;

public interface RoleRepository {

    Role create(Role role);
    Optional<Role> findById(Long id);
    void update(Role role);
    void delete(Long id);
    Set<Role> findAll();
    void assignPermission(Long role, Long permission);
    void unassignPermission(Long role, Long permission);
}
