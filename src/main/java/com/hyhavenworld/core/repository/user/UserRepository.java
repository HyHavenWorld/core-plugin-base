package com.hyhavenworld.core.repository.user;

import com.hyhavenworld.core.domain.Permission;
import com.hyhavenworld.core.domain.Role;
import com.hyhavenworld.core.domain.User;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UserRepository {

    // ===== CRUD básico de usuarios =====
    /**
     * Crea un nuevo usuario en la base de datos
     */
    User create(User user);

    /**
     * Obtiene un usuario por UUID
     */
    Optional<User> getUser(UUID uuid);

    /**
     * Obtiene un usuario por nombre
     */
    Optional<User> getUserByUsername(String username);

    /**
     * Actualiza información del usuario (username, last_seen, hours_played)
     */
    void update(User user);

    /**
     * Elimina un usuario por UUID
     */
    void delete(UUID uuid);

    /**
     * Obtiene todos los usuarios
     */
    Set<User> findAll();

    // ===== Gestión de roles (tabla user_roles) =====
    /**
     * Asigna un rol a un usuario
     */
    void addRole(UUID uuid, String roleName);

    /**
     * Remueve un rol de un usuario
     */
    void removeRole(UUID uuid, String roleName);

    /**
     * Obtiene todos los roles asignados directamente a un usuario
     */
    Set<Role> getRoles(UUID uuid);

    // ===== Gestión de permisos directos (tabla user_permissions) =====
    /**
     * Asigna un permiso directo a un usuario (sobrescribe permisos de roles)
     */
    void addPermission(UUID uuid, String permissionNode, boolean value);

    /**
     * Remueve un permiso directo de un usuario
     */
    void removePermission(UUID uuid, String permissionNode);

    /**
     * Obtiene todos los permisos directos de un usuario (sin incluir permisos de roles)
     */
    Set<Permission> getPermissions(UUID uuid);

    // ===== Métodos de actualización de actividad =====
    /**
     * Actualiza el timestamp de last_seen
     */
    void updateLastSeen(UUID uuid, LocalDateTime timestamp);

    /**
     * Incrementa las horas jugadas
     */
    void updateHoursPlayed(UUID uuid, long hours);
}
