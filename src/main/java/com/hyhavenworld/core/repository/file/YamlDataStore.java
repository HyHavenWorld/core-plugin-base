package com.hyhavenworld.core.repository.file;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe YAML data store for file-based persistence.
 * Manages reading and writing of the entire data structure to a YAML file.
 */
public class YamlDataStore {

    private final String filePath;
    private final Yaml yaml;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private Map<String, Object> data;

    public YamlDataStore(String filePath) {
        this.filePath = filePath;

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        this.yaml = new Yaml(options);

        initializeDataStore();
    }

    private void initializeDataStore() {
        lock.writeLock().lock();
        try {
            Path path = Paths.get(filePath);

            if (Files.exists(path)) {
                loadFromFile();
            } else {
                createEmptyDataStore();
                saveToFile();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadFromFile() {
        try (InputStream input = new FileInputStream(filePath)) {
            Object loaded = yaml.load(input);
            if (loaded instanceof Map) {
                this.data = (Map<String, Object>) loaded;
            } else {
                createEmptyDataStore();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load YAML file: " + filePath, e);
        }
    }

    private void createEmptyDataStore() {
        this.data = new HashMap<>();
        data.put("users", new HashMap<String, Object>());
        data.put("roles", new HashMap<String, Object>());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("next_role_id", 1L);
        data.put("metadata", metadata);
    }

    private void saveToFile() {
        try {
            Path path = Paths.get(filePath);

            // Create parent directories if they exist (path may be just a filename)
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            try (Writer writer = new FileWriter(filePath)) {
                yaml.dump(data, writer);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save YAML file: " + filePath, e);
        }
    }

    /**
     * Read data with a write lock
     * Reloads from file first to ensure consistency when multiple instances exist
     */
    public <T> T read(DataReader<T> reader) {
        lock.writeLock().lock();
        try {
            // Reload from file to get latest changes from other instances
            loadFromFile();
            return reader.read(data);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Write data with a write lock and save to file
     * Reloads from file first to ensure consistency when multiple instances exist
     */
    public void write(DataWriter writer) {
        lock.writeLock().lock();
        try {
            // Reload from file to get latest changes from other instances
            loadFromFile();
            writer.write(data);
            saveToFile();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Reload data from file
     */
    public void reload() {
        lock.writeLock().lock();
        try {
            loadFromFile();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @FunctionalInterface
    public interface DataReader<T> {
        T read(Map<String, Object> data);
    }

    @FunctionalInterface
    public interface DataWriter {
        void write(Map<String, Object> data);
    }

    /**
     * Get next available role ID and increment
     */
    @SuppressWarnings("unchecked")
    public Long getNextRoleId() {
        return read(data -> {
            Map<String, Object> metadata = (Map<String, Object>) data.get("metadata");
            Object nextId = metadata.get("next_role_id");

            if (nextId instanceof Integer) {
                return ((Integer) nextId).longValue();
            } else if (nextId instanceof Long) {
                return (Long) nextId;
            } else {
                return 1L;
            }
        });
    }

    @SuppressWarnings("unchecked")
    public void incrementRoleId() {
        write(data -> {
            Map<String, Object> metadata = (Map<String, Object>) data.get("metadata");
            Object nextId = metadata.get("next_role_id");
            final Long currentId;
            if (nextId instanceof Integer) {
                currentId = ((Integer) nextId).longValue();
            } else if (nextId instanceof Long) {
                currentId = (Long) nextId;
            } else {
                currentId = 1L;
            }
            metadata.put("next_role_id", currentId + 1);
        });
    }
}