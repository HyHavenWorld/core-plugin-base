package com.hyhavenworld.core.repository.user;

import com.hyhavenworld.core.database.CorePersistenceException;
import com.hyhavenworld.core.domain.Permission;
import com.hyhavenworld.core.domain.Role;
import com.hyhavenworld.core.domain.User;
import com.hyhavenworld.core.repository.file.YamlDataStore;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * File-based implementation of UserRepository using YAML storage.
 */
public class UserRepositoryImplFile implements UserRepository {

    private final YamlDataStore dataStore;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public UserRepositoryImplFile(String filePath) {
        this.dataStore = new YamlDataStore(filePath);
    }

    // ===== CRUD básico de usuarios =====

    @Override
    @SuppressWarnings("unchecked")
    public User create(User user) throws CorePersistenceException {
        dataStore.write(data -> {
            Map<String, Object> users = (Map<String, Object>) data.get("users");

            if (users.containsKey(user.getUuid())) {
                throw new CorePersistenceException("User already exists: " + user.getUuid());
            }

            Map<String, Object> userData = new HashMap<>();
            userData.put("username", user.getUsername());
            userData.put("created_at", LocalDateTime.now().format(FORMATTER));
            userData.put("last_seen", LocalDateTime.now().format(FORMATTER));
            userData.put("hours_played", 0L);
            userData.put("roles", new ArrayList<String>());
            userData.put("permissions", new HashMap<String, Boolean>());

            users.put(user.getUuid(), userData);
        });

        return getUser(UUID.fromString(user.getUuid())).orElseThrow();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<User> getUser(UUID uuid) throws CorePersistenceException {
        return dataStore.read(data -> {
            Map<String, Object> users = (Map<String, Object>) data.get("users");
            Map<String, Object> userData = (Map<String, Object>) users.get(uuid.toString());

            if (userData == null) {
                return Optional.empty();
            }

            return Optional.of(mapToUser(uuid.toString(), userData));
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<User> getUserByUsername(String username) throws CorePersistenceException {
        return dataStore.read(data -> {
            Map<String, Object> users = (Map<String, Object>) data.get("users");

            for (Map.Entry<String, Object> entry : users.entrySet()) {
                Map<String, Object> userData = (Map<String, Object>) entry.getValue();
                if (username.equals(userData.get("username"))) {
                    return Optional.of(mapToUser(entry.getKey(), userData));
                }
            }

            return Optional.empty();
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public void update(User user) throws CorePersistenceException {
        dataStore.write(data -> {
            Map<String, Object> users = (Map<String, Object>) data.get("users");
            Map<String, Object> userData = (Map<String, Object>) users.get(user.getUuid());

            if (userData == null) {
                throw new CorePersistenceException("User not found: " + user.getUuid());
            }

            userData.put("username", user.getUsername());
            userData.put("last_seen", user.getLastSeen().format(FORMATTER));
            userData.put("hours_played", user.getHoursPlayed());
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public void delete(UUID uuid) throws CorePersistenceException {
        dataStore.write(data -> {
            Map<String, Object> users = (Map<String, Object>) data.get("users");

            if (!users.containsKey(uuid.toString())) {
                throw new CorePersistenceException("User not found: " + uuid);
            }

            users.remove(uuid.toString());
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<User> findAll() throws CorePersistenceException {
        return dataStore.read(data -> {
            Map<String, Object> users = (Map<String, Object>) data.get("users");
            Set<User> result = new HashSet<>();

            for (Map.Entry<String, Object> entry : users.entrySet()) {
                Map<String, Object> userData = (Map<String, Object>) entry.getValue();
                result.add(mapToUser(entry.getKey(), userData));
            }

            return result;
        });
    }

    // ===== Gestión de roles =====

    @Override
    @SuppressWarnings("unchecked")
    public void addRole(UUID uuid, String roleName) throws CorePersistenceException {
        dataStore.write(data -> {
            Map<String, Object> users = (Map<String, Object>) data.get("users");
            Map<String, Object> userData = (Map<String, Object>) users.get(uuid.toString());

            if (userData == null) {
                throw new CorePersistenceException("User not found: " + uuid);
            }

            List<String> roles = (List<String>) userData.get("roles");
            if (!roles.contains(roleName)) {
                roles.add(roleName);
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public void removeRole(UUID uuid, String roleName) throws CorePersistenceException {
        dataStore.write(data -> {
            Map<String, Object> users = (Map<String, Object>) data.get("users");
            Map<String, Object> userData = (Map<String, Object>) users.get(uuid.toString());

            if (userData == null) {
                throw new CorePersistenceException("User not found: " + uuid);
            }

            List<String> roles = (List<String>) userData.get("roles");
            roles.remove(roleName);
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<Role> getRoles(UUID uuid) throws CorePersistenceException {
        return dataStore.read(data -> {
            Map<String, Object> users = (Map<String, Object>) data.get("users");
            Map<String, Object> userData = (Map<String, Object>) users.get(uuid.toString());

            if (userData == null) {
                return Set.of();
            }

            List<String> roleNames = (List<String>) userData.get("roles");
            Map<String, Object> allRoles = (Map<String, Object>) data.get("roles");
            Set<Role> roles = new HashSet<>();

            for (String roleName : roleNames) {
                Map<String, Object> roleData = (Map<String, Object>) allRoles.get(roleName);
                if (roleData != null) {
                    roles.add(mapToRole(roleName, roleData));
                }
            }

            return roles;
        });
    }

    // ===== Gestión de permisos directos =====

    @Override
    @SuppressWarnings("unchecked")
    public void addPermission(UUID uuid, String permissionNode, boolean value) throws CorePersistenceException {
        dataStore.write(data -> {
            Map<String, Object> users = (Map<String, Object>) data.get("users");
            Map<String, Object> userData = (Map<String, Object>) users.get(uuid.toString());

            if (userData == null) {
                throw new CorePersistenceException("User not found: " + uuid);
            }

            Map<String, Boolean> permissions = (Map<String, Boolean>) userData.get("permissions");
            permissions.put(permissionNode, value);
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public void removePermission(UUID uuid, String permissionNode) throws CorePersistenceException {
        dataStore.write(data -> {
            Map<String, Object> users = (Map<String, Object>) data.get("users");
            Map<String, Object> userData = (Map<String, Object>) users.get(uuid.toString());

            if (userData == null) {
                throw new CorePersistenceException("User not found: " + uuid);
            }

            Map<String, Boolean> permissions = (Map<String, Boolean>) userData.get("permissions");
            permissions.remove(permissionNode);
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<Permission> getPermissions(UUID uuid) throws CorePersistenceException {
        return dataStore.read(data -> {
            Map<String, Object> users = (Map<String, Object>) data.get("users");
            Map<String, Object> userData = (Map<String, Object>) users.get(uuid.toString());

            if (userData == null) {
                return Set.of();
            }

            Map<String, Boolean> permissionsMap = (Map<String, Boolean>) userData.get("permissions");
            Set<Permission> permissions = new HashSet<>();

            for (Map.Entry<String, Boolean> entry : permissionsMap.entrySet()) {
                permissions.add(new Permission(entry.getKey(), entry.getValue()));
            }

            return permissions;
        });
    }

    // ===== Métodos de actualización de actividad =====

    @Override
    @SuppressWarnings("unchecked")
    public void updateLastSeen(UUID uuid, LocalDateTime timestamp) throws CorePersistenceException {
        dataStore.write(data -> {
            Map<String, Object> users = (Map<String, Object>) data.get("users");
            Map<String, Object> userData = (Map<String, Object>) users.get(uuid.toString());

            if (userData == null) {
                throw new CorePersistenceException("User not found: " + uuid);
            }

            userData.put("last_seen", timestamp.format(FORMATTER));
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public void updateHoursPlayed(UUID uuid, long hours) throws CorePersistenceException {
        dataStore.write(data -> {
            Map<String, Object> users = (Map<String, Object>) data.get("users");
            Map<String, Object> userData = (Map<String, Object>) users.get(uuid.toString());

            if (userData == null) {
                throw new CorePersistenceException("User not found: " + uuid);
            }

            userData.put("hours_played", hours);
        });
    }

    // ===== Helper methods =====

    @SuppressWarnings("unchecked")
    private User mapToUser(String uuid, Map<String, Object> userData) {
        String username = (String) userData.get("username");
        LocalDateTime createdAt = LocalDateTime.parse((String) userData.get("created_at"), FORMATTER);
        LocalDateTime lastSeen = LocalDateTime.parse((String) userData.get("last_seen"), FORMATTER);

        Object hoursObj = userData.get("hours_played");
        Long hoursPlayed = hoursObj instanceof Integer ? ((Integer) hoursObj).longValue() : (Long) hoursObj;

        // Get roles
        List<String> roleNames = (List<String>) userData.get("roles");
        Set<Role> roles = new HashSet<>();
        // Note: roles are loaded separately to avoid recursion

        // Get permissions
        Map<String, Boolean> permissionsMap = (Map<String, Boolean>) userData.get("permissions");
        Set<Permission> permissions = new HashSet<>();
        for (Map.Entry<String, Boolean> entry : permissionsMap.entrySet()) {
            permissions.add(new Permission(entry.getKey(), entry.getValue()));
        }

        return new User(uuid, username, createdAt, lastSeen, hoursPlayed, roles, permissions);
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
}