package com.hyhavenworld.core;

import com.hyhavenworld.core.api.PermissionService;
import com.hyhavenworld.core.api.RoleService;
import com.hyhavenworld.core.api.UserService;
import com.hyhavenworld.core.config.CoreConfig;
import com.hyhavenworld.core.config.ServiceRegistry;
import com.hyhavenworld.core.config.StorageType;
import com.hyhavenworld.core.database.StorageManager;

/**
 * Main entry point for the Core Plugin library.
 * Provides a simple facade for initialization, service access, and cleanup.
 *
 * Usage in a plugin:
 * <pre>
 * public void onEnable() {
 *     CorePluginManager.initialize();
 *     // Use services via CorePluginManager.get().users(), etc.
 * }
 *
 * public void onDisable() {
 *     CorePluginManager.shutdown();
 * }
 * </pre>
 */
public class CorePluginManager implements AutoCloseable {

    private static CorePluginManager instance;

    private final CoreConfig config;
    private final ServiceRegistry serviceRegistry;
    private StorageManager storageManager;

    private CorePluginManager(CoreConfig config) {
        this.config = config;
        initializeStorage();
        this.serviceRegistry = new ServiceRegistry(config);
    }

    private void initializeStorage() {
        if (config.getStorageType() == StorageType.DATABASE) {
            StorageManager.init(config);
            this.storageManager = StorageManager.get();
        }
        // FILE storage doesn't need initialization, repositories handle it
    }

    /**
     * Initialize the Core Plugin Manager with a custom configuration.
     * This must be called once before accessing services, typically in onEnable().
     *
     * @param config Custom CoreConfig instance
     * @throws IllegalStateException if already initialized
     */
    public static synchronized void initialize(CoreConfig config) {
        if (instance != null) {
            throw new IllegalStateException("CorePluginManager is already initialized. Call shutdown() first.");
        }
        instance = new CorePluginManager(config);
    }

    /**
     * Initialize the Core Plugin Manager with default configuration.
     * Loads configuration from application.yml in the working directory.
     * This must be called once before accessing services, typically in onEnable().
     *
     * @throws IllegalStateException if already initialized
     */
    public static void initialize() {
        initialize(new CoreConfig());
    }

    /**
     * Get the singleton instance of CorePluginManager.
     *
     * @return the CorePluginManager instance
     * @throws IllegalStateException if not initialized
     */
    public static CorePluginManager get() {
        if (instance == null) {
            throw new IllegalStateException("CorePluginManager is not initialized. Call initialize() first.");
        }
        return instance;
    }

    /**
     * Shutdown the Core Plugin Manager and release all resources.
     * This should be called in onDisable() to properly clean up database connections,
     * file handles, and other resources.
     */
    public static synchronized void shutdown() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }

    /**
     * Close all resources. Called automatically by shutdown().
     * Implements AutoCloseable for try-with-resources support.
     */
    @Override
    public void close() {
        // Close database connections if using database storage
        if (storageManager != null) {
            storageManager.close();
        }

        // Reset service registry
        ServiceRegistry.reset();
    }

    // ===== Service Access =====

    /**
     * Get the UserService for user management operations.
     *
     * @return UserService instance
     */
    public UserService users() {
        return serviceRegistry.users();
    }

    /**
     * Get the RoleService for role management operations.
     *
     * @return RoleService instance
     */
    public RoleService roles() {
        return serviceRegistry.roles();
    }

    /**
     * Get the PermissionService for permission checking operations.
     *
     * @return PermissionService instance
     */
    public PermissionService permissions() {
        return serviceRegistry.permissions();
    }

    /**
     * Get the configuration used by this manager.
     *
     * @return CoreConfig instance
     */
    public CoreConfig getConfig() {
        return config;
    }

    /**
     * Check if the manager is using database storage.
     *
     * @return true if using database, false if using file storage
     */
    public boolean isDatabaseMode() {
        return config.getStorageType() == StorageType.DATABASE;
    }

    /**
     * Check if the Core Plugin Manager is initialized.
     *
     * @return true if initialized, false otherwise
     */
    public static boolean isInitialized() {
        return instance != null;
    }
}