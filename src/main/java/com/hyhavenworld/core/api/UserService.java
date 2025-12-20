package com.hyhavenworld.core.api;

import com.hyhavenworld.core.domain.Role;
import com.hyhavenworld.core.domain.User;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UserService {

    /**
     * Method retrieves a user given its UUID
     * @param playerId the player UUID
     * @return Optional containing user if found
     */
    Optional<User> getUser(UUID playerId);

    /**
     * Creates a new user with the given UUID and username.
     * Timestamps (createdAt, lastSeen) are set to current time.
     * Hours played is initialized to 0.
     *
     * @param playerId the UUID for the new user
     * @param username the username for the new user
     * @return the created User object with all fields populated
     */
    User createUser(UUID playerId, String username);

    /**
     * Method that add a role to the given player
     * @param playerId the player UUID
     * @param roleKey the role key
     */
    void addRole(UUID playerId, String roleKey);

    /**
     * Method that removes a role to the given player
     * @param playerId the player UUID
     * @param roleKey the role key
     */
    void removeRole(UUID playerId, String roleKey);

    /**
     * Method that retrieve roles given player UUID
     * @param playerId the player UUID
     * @return a set of roles assigned to the player
     */
    Set<Role> getRoles(UUID playerId);

    /**
     * Method that retrieves direct permissions assigned to the player.
     * Note: This returns only direct permissions, not permissions inherited from roles.
     * @param playerId the player UUID
     * @return a set of permissions directly assigned to the player
     */
    Set<com.hyhavenworld.core.domain.Permission> getPermissions(UUID playerId);

    /**
     * Method that adds a direct permission to the player.
     * @param playerId the player UUID
     * @param permissionNode the permission node (e.g., "hyhavenworld.admin")
     * @param value true to grant permission, false to deny
     */
    void addPermission(UUID playerId, String permissionNode, boolean value);

    /**
     * Method that removes a direct permission from the player.
     * @param playerId the player UUID
     * @param permissionNode the permission node to remove
     */
    void removePermission(UUID playerId, String permissionNode);
}
