package com.hyhavenworld.core.database;

import com.hyhavenworld.core.config.CoreConfig;

/**
 * Manages database storage and connection pooling.
 * This class is typically not used directly by plugins.
 * Use CorePluginManager instead for a simpler API.
 */
public class StorageManager implements AutoCloseable {
    private static StorageManager instance;

    private final DatabaseManager db;

    private StorageManager(DatabaseManager db) {
        this.db = db;
    }

    /**
     * Initialize the StorageManager with the given configuration.
     * Called by CorePluginManager during initialization.
     *
     * @param config CoreConfig with database settings
     * @throws CorePersistenceException if already initialized
     */
    public static void init(CoreConfig config) {
        if (instance != null) {
            throw new CorePersistenceException("StorageManager already initialized");
        }
        DatabaseManager db = new DatabaseManager();
        db.init(config);

        instance = new StorageManager(db);
    }

    public static StorageManager get() {
        if (instance == null) {
            throw new CorePersistenceException("StorageManager not initialized");
        }
        return instance;
    }

    public DatabaseManager getDatabase() {
        return db;
    }

    /**
     * Close database connections and release resources.
     * Called automatically by CorePluginManager.shutdown().
     */
    @Override
    public void close() {
        if (db != null) {
            db.close();
        }
        instance = null;
    }

    /**
     * FOR TESTING ONLY - Sets a custom instance for testing purposes
     * This allows tests to inject a mock DatabaseManager
     */
    public static void setInstanceForTesting(DatabaseManager testDatabaseManager) {
        instance = new StorageManager(testDatabaseManager);
    }

    /**
     * FOR TESTING ONLY - Resets the singleton instance
     */
    public static void resetForTesting() {
        instance = null;
    }
}
