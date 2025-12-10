package com.hyhavenworld.core.config;

public class StorageConfig {
    private final StorageType storageType;

    public StorageConfig(StorageType type) {
        this.storageType = type;
    }

    public boolean usesDatabase() {
        return storageType == StorageType.DATABASE;
    }

    public boolean usesFile() {
        return storageType == StorageType.FILE;
    }

    public StorageType getType() {
        return storageType;
    }
}
