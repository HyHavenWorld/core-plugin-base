package com.hyhavenworld.core.repository.file;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class YamlDataStoreTest {

    private Path tempFile;
    private YamlDataStore dataStore;

    @BeforeEach
    void setUp() throws IOException {
        tempFile = Files.createTempFile("test-yaml-", ".yml");
        dataStore = new YamlDataStore(tempFile.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempFile != null && Files.exists(tempFile)) {
            Files.delete(tempFile);
        }
    }

    @Test
    void testInitialStructure() {
        dataStore.read(data -> {
            assertNotNull(data);
            assertTrue(data.containsKey("users"));
            assertTrue(data.containsKey("roles"));
            assertTrue(data.containsKey("metadata"));
            return null;
        });
    }

    @Test
    void testReadOperation() {
        String result = dataStore.read(data -> {
            Map<String, Object> metadata = (Map<String, Object>) data.get("metadata");
            Object nextRoleId = metadata.get("next_role_id");
            return nextRoleId.toString();
        });

        assertEquals("1", result);
    }

    @Test
    void testWriteOperation() {
        dataStore.write(data -> {
            Map<String, Object> users = (Map<String, Object>) data.get("users");
            Map<String, Object> userData = new HashMap<>();
            userData.put("username", "testuser");
            users.put("test-uuid", userData);
        });

        // Verify write persisted
        String username = dataStore.read(data -> {
            Map<String, Object> users = (Map<String, Object>) data.get("users");
            Map<String, Object> userData = (Map<String, Object>) users.get("test-uuid");
            return (String) userData.get("username");
        });

        assertEquals("testuser", username);
    }

    @Test
    void testReloadFromFile() throws IOException {
        // Create first instance and write data
        dataStore.write(data -> {
            Map<String, Object> users = (Map<String, Object>) data.get("users");
            Map<String, Object> userData = new HashMap<>();
            userData.put("username", "user1");
            users.put("uuid1", userData);
        });

        // Create second instance pointing to same file
        YamlDataStore dataStore2 = new YamlDataStore(tempFile.toString());

        // Read from second instance (should reload from file)
        String username = dataStore2.read(data -> {
            Map<String, Object> users = (Map<String, Object>) data.get("users");
            Map<String, Object> userData = (Map<String, Object>) users.get("uuid1");
            return (String) userData.get("username");
        });

        assertEquals("user1", username);
    }

    @Test
    void testReloadMethod() {
        // Write data
        dataStore.write(data -> {
            Map<String, Object> users = (Map<String, Object>) data.get("users");
            Map<String, Object> userData = new HashMap<>();
            userData.put("username", "original");
            users.put("uuid1", userData);
        });

        // Manually modify file (simulate external change)
        YamlDataStore externalStore = new YamlDataStore(tempFile.toString());
        externalStore.write(data -> {
            Map<String, Object> users = (Map<String, Object>) data.get("users");
            Map<String, Object> userData = (Map<String, Object>) users.get("uuid1");
            userData.put("username", "modified");
        });

        // Reload original store
        dataStore.reload();

        // Should see the modified value
        String username = dataStore.read(data -> {
            Map<String, Object> users = (Map<String, Object>) data.get("users");
            Map<String, Object> userData = (Map<String, Object>) users.get("uuid1");
            return (String) userData.get("username");
        });

        assertEquals("modified", username);
    }

    @Test
    void testGetNextRoleId() {
        Long id1 = dataStore.getNextRoleId();
        assertEquals(1L, id1);

        dataStore.incrementRoleId();

        Long id2 = dataStore.getNextRoleId();
        assertEquals(2L, id2);
    }

    @Test
    void testIncrementRoleId() {
        dataStore.incrementRoleId();
        dataStore.incrementRoleId();
        dataStore.incrementRoleId();

        Long id = dataStore.getNextRoleId();
        assertEquals(4L, id);
    }

    @Test
    void testConcurrentReads() throws InterruptedException {
        // Prepare data
        dataStore.write(data -> {
            Map<String, Object> users = (Map<String, Object>) data.get("users");
            for (int i = 0; i < 100; i++) {
                Map<String, Object> userData = new HashMap<>();
                userData.put("username", "user" + i);
                users.put("uuid" + i, userData);
            }
        });

        // Concurrent reads
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        int finalJ = j;
                        String username = dataStore.read(data -> {
                            Map<String, Object> users = (Map<String, Object>) data.get("users");
                            Map<String, Object> userData = (Map<String, Object>) users.get("uuid" + (finalJ % 100));
                            return (String) userData.get("username");
                        });
                        if (username != null && username.startsWith("user")) {
                            successCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        assertEquals(threadCount * 100, successCount.get());
    }

    @Test
    void testConcurrentWrites() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    dataStore.write(data -> {
                        Map<String, Object> users = (Map<String, Object>) data.get("users");
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("username", "user" + index);
                        users.put("uuid" + index, userData);
                    });
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Verify all writes succeeded
        Integer userCount = dataStore.read(data -> {
            Map<String, Object> users = (Map<String, Object>) data.get("users");
            return users.size();
        });

        assertEquals(threadCount, userCount);
    }

    @Test
    void testConcurrentReadWrite() throws InterruptedException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errors = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    if (index % 2 == 0) {
                        // Write operation
                        dataStore.write(data -> {
                            Map<String, Object> users = (Map<String, Object>) data.get("users");
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("username", "user" + index);
                            users.put("uuid" + index, userData);
                        });
                    } else {
                        // Read operation
                        dataStore.read(data -> {
                            Map<String, Object> users = (Map<String, Object>) data.get("users");
                            return users.size();
                        });
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        assertEquals(0, errors.get(), "Should have no errors in concurrent read/write");
    }

    @Test
    void testFileCreatedIfNotExists() throws IOException {
        Path newFile = Files.createTempFile("new-yaml-", ".yml");
        Files.delete(newFile); // Delete it so YamlDataStore creates it

        YamlDataStore newStore = new YamlDataStore(newFile.toString());

        // Verify file was created
        assertTrue(Files.exists(newFile));

        // Verify structure was initialized
        newStore.read(data -> {
            assertNotNull(data);
            assertTrue(data.containsKey("users"));
            assertTrue(data.containsKey("roles"));
            assertTrue(data.containsKey("metadata"));
            return null;
        });

        // Cleanup
        Files.delete(newFile);
    }

    @Test
    void testMultipleInstancesReloadCorrectly() {
        // Instance 1 writes
        dataStore.write(data -> {
            Map<String, Object> users = (Map<String, Object>) data.get("users");
            Map<String, Object> userData = new HashMap<>();
            userData.put("username", "instance1");
            users.put("uuid1", userData);
        });

        // Instance 2 (same file) reads - should reload
        YamlDataStore dataStore2 = new YamlDataStore(tempFile.toString());
        String username = dataStore2.read(data -> {
            Map<String, Object> users = (Map<String, Object>) data.get("users");
            Map<String, Object> userData = (Map<String, Object>) users.get("uuid1");
            return (String) userData.get("username");
        });

        assertEquals("instance1", username);

        // Instance 2 writes
        dataStore2.write(data -> {
            Map<String, Object> users = (Map<String, Object>) data.get("users");
            Map<String, Object> userData = new HashMap<>();
            userData.put("username", "instance2");
            users.put("uuid2", userData);
        });

        // Instance 1 reads - should reload
        String username2 = dataStore.read(data -> {
            Map<String, Object> users = (Map<String, Object>) data.get("users");
            Map<String, Object> userData = (Map<String, Object>) users.get("uuid2");
            return (String) userData.get("username");
        });

        assertEquals("instance2", username2);
    }
}