package com.hyhavenworld.core.config;

import com.hyhavenworld.core.database.DatabaseManager;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;

public class CoreConfig {
    private final StorageType storageType;
    private final String filePath;
    private final DatabaseConfig databaseConfig;

    public CoreConfig(){
        Config config = ConfigFactory.parseFile(new File("application.yml")).resolve();
        this.storageType = StorageType.valueOf(config.getString("storageType").toUpperCase());
        this.filePath = config.getString("file.path");
        this.databaseConfig = new DatabaseConfig(config);
    }

    public StorageType getStorageType() { return storageType; }
    public String getFilePath() { return filePath; }
    public DatabaseConfig getDatabaseConfig() { return databaseConfig; }
}
