package com.hyhavenworld.core.repository.role;

import com.hyhavenworld.core.domain.Role;

import java.util.Optional;
import java.util.Set;

public interface RoleRepository {

    Role createRole(Role role);
    Optional<Role> getRole(int id);
    void addInheritance(String parent, String child);

}
