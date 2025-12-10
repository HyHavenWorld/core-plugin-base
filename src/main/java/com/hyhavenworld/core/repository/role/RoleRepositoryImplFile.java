package com.hyhavenworld.core.repository.role;

import com.hyhavenworld.core.domain.Role;

import java.util.Optional;
import java.util.Set;

public class RoleRepositoryImplFile implements RoleRepository{

    public RoleRepositoryImplFile(String filePath) {}

    @Override
    public Role createRole(Role role) {
        return null;
    }

    @Override
    public Optional<Role> getRole(int id) {
        return Optional.empty();
    }

    @Override
    public void addInheritance(String parent, String child) {

    }

    @Override
    public void update(Role role) {

    }

    @Override
    public void delete(Long id) {

    }

    @Override
    public Set<Role> findAll() {
        return Set.of();
    }

    @Override
    public void assignPermission(Long role, Long permission) {

    }

    @Override
    public void unassignPermission(Long role, Long permission) {

    }
}
