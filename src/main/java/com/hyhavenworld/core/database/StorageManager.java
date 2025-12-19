package com.hyhavenworld.core.database;

import com.hyhavenworld.core.config.CoreConfig;

public class StorageManager {
    private static StorageManager instance;

    private final DatabaseManager db;

    private StorageManager(DatabaseManager db) {
        this.db = db;
    }

    public static void init(CoreConfig config) {
        if (instance != null) throw new CorePersistenceException("StorageManager already initialized");
        DatabaseManager db = new DatabaseManager();
        db.init(config);

        instance = new StorageManager(db);
    }

    public static StorageManager get() {
        return instance;
    }

    public DatabaseManager getDatabase() {
        return db;
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
