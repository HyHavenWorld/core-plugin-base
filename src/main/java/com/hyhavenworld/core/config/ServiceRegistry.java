package com.hyhavenworld.core.config;

import com.hyhavenworld.core.api.PermissionService;
import com.hyhavenworld.core.api.RoleService;
import com.hyhavenworld.core.api.UserService;
import com.hyhavenworld.core.service.PermissionServiceImpl;
import com.hyhavenworld.core.service.RoleServiceImpl;
import com.hyhavenworld.core.service.UserServiceImpl;

/**
 * Registry for core services (User, Role, Permission).
 * Manages service instances and their lifecycle.
 *
 * Note: This class is typically not used directly by plugins.
 * Use CorePluginManager instead for a simpler API.
 */
public class ServiceRegistry {

    private final UserService userService;
    private final RoleService roleService;
    private final PermissionService permissionService;

    /**
     * Public constructor. Services are created based on the provided configuration.
     * This class is typically instantiated by CorePluginManager.
     *
     * @param config CoreConfig with storage type and settings
     */
    public ServiceRegistry(CoreConfig config) {
        this.userService = new UserServiceImpl(config);
        this.roleService = new RoleServiceImpl(config);
        this.permissionService = new PermissionServiceImpl(config);
    }

    public UserService users() {
        return userService;
    }

    public RoleService roles() {
        return roleService;
    }

    public PermissionService permissions() {
        return permissionService;
    }

    /**
     * Reset the registry. Called during shutdown to clean up resources.
     * Public static method called by CorePluginManager.
     */
    public static void reset() {
        // Services don't hold resources, but this method exists for future extensibility
        // and to maintain symmetry with initialization
    }
}
