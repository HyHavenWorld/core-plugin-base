package com.hyhavenworld.core.config;

import com.hyhavenworld.core.api.PermissionService;
import com.hyhavenworld.core.api.RoleService;
import com.hyhavenworld.core.api.UserService;
import com.hyhavenworld.core.service.PermissionServiceImpl;
import com.hyhavenworld.core.service.RoleServiceImpl;
import com.hyhavenworld.core.service.UserServiceImpl;

public class ServiceRegistry {

    private static ServiceRegistry instance;

    private final UserService userService;
    private final RoleService roleService;
    private final PermissionService permissionService;

    private ServiceRegistry() {
        CoreConfig coreConfig = new CoreConfig();
        this.userService = new UserServiceImpl(coreConfig);
        this.roleService = new RoleServiceImpl(coreConfig);
        this.permissionService = new PermissionServiceImpl(coreConfig);
    }

    public static void init() {
        if (instance != null) {
            throw new RegistryException("ServiceRegistry already initialized");
        }

        instance = new ServiceRegistry();
    }

    public static ServiceRegistry get() {
        if (instance == null) {
            throw new RegistryException("ServiceRegistry must be initialized first");
        }
        return instance;
    }

    public UserService users() { return userService; }
    public RoleService roles() { return roleService; }
    public PermissionService permissions() { return permissionService; }
}
