package com.hyhavenworld.core.repository.role;

import com.hyhavenworld.core.domain.Permission;
import com.hyhavenworld.core.domain.Role;

import java.util.Optional;
import java.util.Set;

public interface RoleRepository {

    // ===== CRUD básico de roles =====
    /**
     * Crea un nuevo rol
     */
    Role create(Role role);

    /**
     * Obtiene un rol por ID
     */
    Optional<Role> getById(Long id);

    /**
     * Obtiene un rol por nombre
     */
    Optional<Role> getByName(String name);

    /**
     * Actualiza el nombre de un rol
     */
    void update(Role role);

    /**
     * Elimina un rol por ID
     */
    void delete(Long id);

    /**
     * Elimina un rol por nombre
     */
    void deleteByName(String name);

    /**
     * Obtiene todos los roles
     */
    Set<Role> findAll();

    // ===== Gestión de permisos de roles (tabla role_permissions) =====
    /**
     * Asigna un permiso a un rol
     */
    void addPermission(String roleName, String permissionNode, boolean value);

    /**
     * Remueve un permiso de un rol
     */
    void removePermission(String roleName, String permissionNode);

    /**
     * Obtiene todos los permisos de un rol (sin incluir herencia)
     */
    Set<Permission> getPermissions(String roleName);

    // ===== Gestión de herencia de roles (tabla role_inheritance) =====
    /**
     * Establece una relación de herencia: child hereda de parent
     */
    void addInheritance(String parentRoleName, String childRoleName);

    /**
     * Remueve una relación de herencia
     */
    void removeInheritance(String parentRoleName, String childRoleName);

    /**
     * Obtiene todos los roles padre directos de un rol
     */
    Set<Role> getParentRoles(String roleName);

    /**
     * Obtiene todos los roles hijo directos de un rol
     */
    Set<Role> getChildRoles(String roleName);

    /**
     * Obtiene todos los permisos efectivos de un rol (incluyendo herencia)
     * Resuelve la herencia recursivamente
     */
    Set<Permission> getEffectivePermissions(String roleName);
}
