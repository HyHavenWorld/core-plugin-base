package com.hyhavenworld.core.service;

import com.hyhavenworld.core.api.RoleService;
import com.hyhavenworld.core.config.CoreConfig;
import com.hyhavenworld.core.domain.Role;
import com.hyhavenworld.core.repository.role.RoleRepository;
import com.hyhavenworld.core.repository.role.RoleRepositoryImplFile;
import com.hyhavenworld.core.repository.role.RoleRepositoryImplJDBC;

import java.util.Optional;

public class RoleServiceImpl implements RoleService {

    private RoleRepository roleRepository;

    public RoleServiceImpl( CoreConfig coreConfig ) {
        this.roleRepository = switch(coreConfig.getStorageType()) {
            case DATABASE ->  new RoleRepositoryImplJDBC();
            case FILE -> new RoleRepositoryImplFile(coreConfig.getFilePath());
            default -> new RoleRepositoryImplFile(coreConfig.getFilePath());
        };
    }

    @Override
    public Optional<Role> getRole(Long id) {
        return this.roleRepository.getById(id);
    }

    @Override
    public void createRole(Role role) {
        this.roleRepository.create(role);
    }

    @Override
    public void addInheritance(String parent, String child) {
        this.roleRepository.addInheritance(parent, child);
    }
}
