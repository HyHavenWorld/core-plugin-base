package com.hyhavenworld.core.api;


import com.hyhavenworld.core.domain.Role;

import java.util.Optional;


public interface RoleService {

    /**
     * Method that get the role given its id
     * @param id the role id
     * @return Optional with the role if found
     */
    Optional<Role> getRole(Long id);

    /**
     * Method that creates a role
     * @param role the role to be created
     */
    void createRole(Role role);

    /**
     * Method that set the inheritance for 2 roles
     * @param parent the parent role
     * @param child the child role
     */
    void addInheritance(String parent, String child);

    /**
     * Method that adds a permission to a role
     * @param roleKey the role identifier (name)
     * @param permissionNode the permission node to add
     * @param value the permission value (true = granted, false = denied)
     */
    void addPermission(String roleKey, String permissionNode, boolean value);

    /**
     * Method that removes a permission from a role
     * @param roleKey the role identifier (name)
     * @param permissionNode the permission node to remove
     */
    void removePermission(String roleKey, String permissionNode);

}
