package com.hyhavenworld.core.service;

import com.hyhavenworld.core.api.RoleService;
import com.hyhavenworld.core.domain.Role;
import com.hyhavenworld.core.repository.role.RoleRepository;

import java.util.Optional;

public class RoleServiceImpl implements RoleService {

    private RoleRepository roleRepository;

    @Override
    public Optional<Role> getRole(int id) {
        return this.roleRepository.getRole(id);
    }

    @Override
    public void createRole(Role role) {
        this.roleRepository.createRole(role);
    }

    @Override
    public void addInheritance(String parent, String child) {
        this.roleRepository.addInheritance(parent, child);
    }
}
