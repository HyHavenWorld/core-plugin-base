package com.hyhavenworld.core.test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Manages an in-memory H2 database for testing purposes.
 * Sets up H2 in MySQL compatibility mode and runs Flyway migrations.
 */
public class TestDatabaseManager {

    private static TestDatabaseManager instance;
    private HikariDataSource dataSource;

    private TestDatabaseManager() {
        initializeDatabase();
    }

    public static TestDatabaseManager getInstance() {
        if (instance == null) {
            instance = new TestDatabaseManager();
        }
        return instance;
    }

    private void initializeDatabase() {
        // Configure H2 in MySQL mode
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:testdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(5);
        config.setAutoCommit(true);

        dataSource = new HikariDataSource(config);

        // Run Flyway migrations - use H2-specific migrations only
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration/h2")
            .cleanDisabled(false)
            .load();

        flyway.migrate();
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void cleanDatabase() {
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration/h2")
            .cleanDisabled(false)
            .load();

        flyway.clean();
        flyway.migrate();
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    /**
     * Resets the singleton instance (useful for test isolation)
     */
    public static void reset() {
        if (instance != null) {
            instance.shutdown();
            instance = null;
        }
    }
}