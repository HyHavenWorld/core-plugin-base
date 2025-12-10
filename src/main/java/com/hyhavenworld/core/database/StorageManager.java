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
}
