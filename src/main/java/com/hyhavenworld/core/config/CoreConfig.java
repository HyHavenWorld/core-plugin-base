package com.hyhavenworld.core.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;

public class CoreConfig {
    private final StorageType storageType;
    private final String filePath;
    private final DatabaseConfig databaseConfig;
    private final CacheConfig cacheConfig;

    public CoreConfig(){
        Config config = ConfigFactory.parseFile(new File("application.conf")).resolve();
        this.storageType = StorageType.valueOf(config.getString("storageType").toUpperCase());

        // Only read file.path if storage type is FILE
        this.filePath = (this.storageType == StorageType.FILE && config.hasPath("file.path"))
            ? config.getString("file.path")
            : null;

        this.databaseConfig = new DatabaseConfig(config);
        this.cacheConfig = new CacheConfig(config);
    }

    public StorageType getStorageType() { return storageType; }
    public String getFilePath() { return filePath; }
    public DatabaseConfig getDatabaseConfig() { return databaseConfig; }
    public CacheConfig getCacheConfig() { return cacheConfig; }
}
