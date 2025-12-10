package com.hyhavenworld.core.api;

import java.util.UUID;

public interface PermissionService {

    /**
     * Method to check if a player has a permission
     * @param playerId the player uuid
     * @param permissionKey the permission key
     * @return boolean indicating if the given player has the permission
     */
    boolean hasPermission(UUID playerId, String permissionKey);
}
