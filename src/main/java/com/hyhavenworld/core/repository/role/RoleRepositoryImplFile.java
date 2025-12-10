package com.hyhavenworld.core.repository.role;

import com.hyhavenworld.core.domain.Role;

import java.util.Optional;

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

}
