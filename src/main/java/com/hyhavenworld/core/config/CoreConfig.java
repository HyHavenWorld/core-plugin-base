package com.hyhavenworld.core.config;

import com.hyhavenworld.core.util.YamlUtil;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class CoreConfig {
    private final StorageType storageType;
    private final String filePath;
    private final DatabaseConfig databaseConfig;

    public CoreConfig(File configFile) throws IOException {
        YamlUtil yamlUtil = new YamlUtil(configFile);
        this.storageType = StorageType.valueOf(yamlUtil.getString("storageType").toUpperCase());
        Map<String, Object> fileConfig = (Map<String, Object>) yamlData.get("file");
        this.filePath = fileConfig.get("path").toString();
        this.databaseConfig = new DatabaseConfig(yaml.getConfigurationSection("database"));
    }

    public StorageType getStorageType() { return storageType; }
    public boolean isDatabaseEnabled() { return databaseEnabled; }
    public String getFilePath() { return filePath; }
    public DatabaseConfig getDatabaseConfig() { return databaseConfig; }
}
