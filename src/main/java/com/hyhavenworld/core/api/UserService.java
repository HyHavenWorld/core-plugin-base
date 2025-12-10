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
}
