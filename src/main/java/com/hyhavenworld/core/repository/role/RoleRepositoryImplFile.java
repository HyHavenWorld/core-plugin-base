package com.hyhavenworld.core.repository.role;

import com.hyhavenworld.core.database.CorePersistenceException;
import com.hyhavenworld.core.domain.Permission;
import com.hyhavenworld.core.domain.Role;
import com.hyhavenworld.core.repository.file.YamlDataStore;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * File-based implementation of RoleRepository using YAML storage.
 */
public class RoleRepositoryImplFile implements RoleRepository {

    private final YamlDataStore dataStore;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public RoleRepositoryImplFile(String filePath) {
        this.dataStore = new YamlDataStore(filePath);
    }

    // ===== CRUD básico de roles =====

    @Override
    @SuppressWarnings("unchecked")
    public Role create(Role role) throws CorePersistenceException {
        Long newId = dataStore.getNextRoleId();

        dataStore.write(data -> {
            Map<String, Object> roles = (Map<String, Object>) data.get("roles");

            if (roles.containsKey(role.getName())) {
                throw new CorePersistenceException("Role already exists: " + role.getName());
            }

            Map<String, Object> roleData = new HashMap<>();
            roleData.put("id", newId);
            roleData.put("created_at", LocalDateTime.now().format(FORMATTER));
            roleData.put("permissions", new HashMap<String, Boolean>());
            roleData.put("parents", new ArrayList<String>());

            roles.put(role.getName(), roleData);
        });

        dataStore.incrementRoleId();

        return getByName(role.getName()).orElseThrow();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Role> getById(Long id) throws CorePersistenceException {
        return dataStore.read(data -> {
            Map<String, Object> roles = (Map<String, Object>) data.get("roles");

            for (Map.Entry<String, Object> entry : roles.entrySet()) {
                Map<String, Object> roleData = (Map<String, Object>) entry.getValue();
                Object roleIdObj = roleData.get("id");
                Long roleId = roleIdObj instanceof Integer ? ((Integer) roleIdObj).longValue() : (Long) roleIdObj;

                if (roleId.equals(id)) {
                    return Optional.of(mapToRole(entry.getKey(), roleData));
                }
            }

            return Optional.empty();
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Role> getByName(String name) throws CorePersistenceException {
        return dataStore.read(data -> {
            Map<String, Object> roles = (Map<String, Object>) data.get("roles");
            Map<String, Object> roleData = (Map<String, Object>) roles.get(name);

            if (roleData == null) {
                return Optional.empty();
            }

            return Optional.of(mapToRole(name, roleData));
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public void update(Role role) throws CorePersistenceException {
        dataStore.write(data -> {
            Map<String, Object> roles = (Map<String, Object>) data.get("roles");

            // Find role by ID
            String oldName = null;
            for (Map.Entry<String, Object> entry : roles.entrySet()) {
                Map<String, Object> roleData = (Map<String, Object>) entry.getValue();
                Object roleIdObj = roleData.get("id");
                Long roleId = roleIdObj instanceof Integer ? ((Integer) roleIdObj).longValue() : (Long) roleIdObj;

                if (roleId.equals(role.getId())) {
                    oldName = entry.getKey();
                    break;
                }
            }

            if (oldName == null) {
                throw new CorePersistenceException("Role not found with ID: " + role.getId());
            }

            // If name changed, move the role data
            if (!oldName.equals(role.getName())) {
                Map<String, Object> roleData = (Map<String, Object>) roles.remove(oldName);
                roles.put(role.getName(), roleData);

                // Update references in user_roles
                Map<String, Object> users = (Map<String, Object>) data.get("users");
                for (Object userDataObj : users.values()) {
                    Map<String, Object> userData = (Map<String, Object>) userDataObj;
                    List<String> userRoles = (List<String>) userData.get("roles");
                    for (int i = 0; i < userRoles.size(); i++) {
                        if (userRoles.get(i).equals(oldName)) {
                            userRoles.set(i, role.getName());
                        }
                    }
                }

                // Update references in role inheritance
                for (Object roleDataObj : roles.values()) {
                    Map<String, Object> rd = (Map<String, Object>) roleDataObj;
                    List<String> parents = (List<String>) rd.get("parents");
                    for (int i = 0; i < parents.size(); i++) {
                        if (parents.get(i).equals(oldName)) {
                            parents.set(i, role.getName());
                        }
                    }
                }
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public void delete(Long id) throws CorePersistenceException {
        dataStore.write(data -> {
            Map<String, Object> roles = (Map<String, Object>) data.get("roles");

            String nameToDelete = null;
            for (Map.Entry<String, Object> entry : roles.entrySet()) {
                Map<String, Object> roleData = (Map<String, Object>) entry.getValue();
                Object roleIdObj = roleData.get("id");
                Long roleId = roleIdObj instanceof Integer ? ((Integer) roleIdObj).longValue() : (Long) roleIdObj;

                if (roleId.equals(id)) {
                    nameToDelete = entry.getKey();
                    break;
                }
            }

            if (nameToDelete == null) {
                throw new CorePersistenceException("Role not found with ID: " + id);
            }

            roles.remove(nameToDelete);

            // Clean up references
            cleanupRoleReferences(data, nameToDelete);
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public void deleteByName(String name) throws CorePersistenceException {
        dataStore.write(data -> {
            Map<String, Object> roles = (Map<String, Object>) data.get("roles");

            if (!roles.containsKey(name)) {
                throw new CorePersistenceException("Role not found: " + name);
            }

            roles.remove(name);

            // Clean up references
            cleanupRoleReferences(data, name);
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<Role> findAll() throws CorePersistenceException {
        return dataStore.read(data -> {
            Map<String, Object> roles = (Map<String, Object>) data.get("roles");
            Set<Role> result = new HashSet<>();

            for (Map.Entry<String, Object> entry : roles.entrySet()) {
                Map<String, Object> roleData = (Map<String, Object>) entry.getValue();
                result.add(mapToRole(entry.getKey(), roleData));
            }

            return result;
        });
    }

    // ===== Gestión de permisos =====

    @Override
    @SuppressWarnings("unchecked")
    public void addPermission(String roleName, String permissionNode, boolean value) throws CorePersistenceException {
        dataStore.write(data -> {
            Map<String, Object> roles = (Map<String, Object>) data.get("roles");
            Map<String, Object> roleData = (Map<String, Object>) roles.get(roleName);

            if (roleData == null) {
                throw new CorePersistenceException("Role not found: " + roleName);
            }

            Map<String, Boolean> permissions = (Map<String, Boolean>) roleData.get("permissions");
            permissions.put(permissionNode, value);
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public void removePermission(String roleName, String permissionNode) throws CorePersistenceException {
        dataStore.write(data -> {
            Map<String, Object> roles = (Map<String, Object>) data.get("roles");
            Map<String, Object> roleData = (Map<String, Object>) roles.get(roleName);

            if (roleData == null) {
                throw new CorePersistenceException("Role not found: " + roleName);
            }

            Map<String, Boolean> permissions = (Map<String, Boolean>) roleData.get("permissions");
            permissions.remove(permissionNode);
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<Permission> getPermissions(String roleName) throws CorePersistenceException {
        return dataStore.read(data -> {
            Map<String, Object> roles = (Map<String, Object>) data.get("roles");
            Map<String, Object> roleData = (Map<String, Object>) roles.get(roleName);

            if (roleData == null) {
                return Set.of();
            }

            Map<String, Boolean> permissionsMap = (Map<String, Boolean>) roleData.get("permissions");
            Set<Permission> permissions = new HashSet<>();

            for (Map.Entry<String, Boolean> entry : permissionsMap.entrySet()) {
                permissions.add(new Permission(entry.getKey(), entry.getValue()));
            }

            return permissions;
        });
    }

    // ===== Gestión de herencia =====

    @Override
    @SuppressWarnings("unchecked")
    public void addInheritance(String parentRoleName, String childRoleName) throws CorePersistenceException {
        dataStore.write(data -> {
            Map<String, Object> roles = (Map<String, Object>) data.get("roles");
            Map<String, Object> childRoleData = (Map<String, Object>) roles.get(childRoleName);

            if (childRoleData == null) {
                throw new CorePersistenceException("Child role not found: " + childRoleName);
            }

            if (!roles.containsKey(parentRoleName)) {
                throw new CorePersistenceException("Parent role not found: " + parentRoleName);
            }

            List<String> parents = (List<String>) childRoleData.get("parents");
            if (!parents.contains(parentRoleName)) {
                parents.add(parentRoleName);
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public void removeInheritance(String parentRoleName, String childRoleName) throws CorePersistenceException {
        dataStore.write(data -> {
            Map<String, Object> roles = (Map<String, Object>) data.get("roles");
            Map<String, Object> childRoleData = (Map<String, Object>) roles.get(childRoleName);

            if (childRoleData == null) {
                throw new CorePersistenceException("Child role not found: " + childRoleName);
            }

            List<String> parents = (List<String>) childRoleData.get("parents");
            parents.remove(parentRoleName);
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<Role> getParentRoles(String roleName) throws CorePersistenceException {
        return dataStore.read(data -> {
            Map<String, Object> roles = (Map<String, Object>) data.get("roles");
            Map<String, Object> roleData = (Map<String, Object>) roles.get(roleName);

            if (roleData == null) {
                return Set.of();
            }

            List<String> parentNames = (List<String>) roleData.get("parents");
            Set<Role> parentRoles = new HashSet<>();

            for (String parentName : parentNames) {
                Map<String, Object> parentData = (Map<String, Object>) roles.get(parentName);
                if (parentData != null) {
                    parentRoles.add(mapToRole(parentName, parentData));
                }
            }

            return parentRoles;
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<Role> getChildRoles(String roleName) throws CorePersistenceException {
        return dataStore.read(data -> {
            Map<String, Object> roles = (Map<String, Object>) data.get("roles");
            Set<Role> childRoles = new HashSet<>();

            for (Map.Entry<String, Object> entry : roles.entrySet()) {
                Map<String, Object> roleData = (Map<String, Object>) entry.getValue();
                List<String> parents = (List<String>) roleData.get("parents");

                if (parents.contains(roleName)) {
                    childRoles.add(mapToRole(entry.getKey(), roleData));
                }
            }

            return childRoles;
        });
    }

    @Override
    public Set<Permission> getEffectivePermissions(String roleName) throws CorePersistenceException {
        Set<Permission> effectivePermissions = new HashSet<>();
        Set<String> visited = new HashSet<>();
        collectPermissionsRecursively(roleName, effectivePermissions, visited);
        return effectivePermissions;
    }

    // ===== Helper methods =====

    private void collectPermissionsRecursively(String roleName, Set<Permission> accumulated, Set<String> visited)
            throws CorePersistenceException {

        if (visited.contains(roleName)) {
            return;
        }
        visited.add(roleName);

        Set<Permission> directPermissions = getPermissions(roleName);
        accumulated.addAll(directPermissions);

        Set<Role> parents = getParentRoles(roleName);
        for (Role parent : parents) {
            collectPermissionsRecursively(parent.getName(), accumulated, visited);
        }
    }

    @SuppressWarnings("unchecked")
    private Role mapToRole(String name, Map<String, Object> roleData) {
        Object idObj = roleData.get("id");
        Long id = idObj instanceof Integer ? ((Integer) idObj).longValue() : (Long) idObj;

        LocalDateTime createdAt = LocalDateTime.parse((String) roleData.get("created_at"), FORMATTER);

        Map<String, Boolean> permissionsMap = (Map<String, Boolean>) roleData.get("permissions");
        Set<Permission> permissions = new HashSet<>();

        for (Map.Entry<String, Boolean> entry : permissionsMap.entrySet()) {
            permissions.add(new Permission(entry.getKey(), entry.getValue()));
        }

        return new Role(id, name, createdAt, permissions);
    }

    @SuppressWarnings("unchecked")
    private void cleanupRoleReferences(Map<String, Object> data, String roleName) {
        // Remove from user roles
        Map<String, Object> users = (Map<String, Object>) data.get("users");
        for (Object userDataObj : users.values()) {
            Map<String, Object> userData = (Map<String, Object>) userDataObj;
            List<String> userRoles = (List<String>) userData.get("roles");
            userRoles.remove(roleName);
        }

        // Remove from role inheritance
        Map<String, Object> roles = (Map<String, Object>) data.get("roles");
        for (Object roleDataObj : roles.values()) {
            Map<String, Object> roleData = (Map<String, Object>) roleDataObj;
            List<String> parents = (List<String>) roleData.get("parents");
            parents.remove(roleName);
        }
    }
}