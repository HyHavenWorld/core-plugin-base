package com.hyhavenworld.core.database;

import com.hyhavenworld.core.config.CoreConfig;
import com.hyhavenworld.core.config.DatabaseConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Manages database connections using HikariCP connection pooling.
 * Handles database initialization and migrations via Flyway.
 */
public class DatabaseManager implements AutoCloseable {

    private HikariDataSource datasource;

    /**
     * Initialize the database connection pool and run migrations.
     * Package-private, only called by StorageManager.
     *
     * @param config CoreConfig with database settings
     */
    void init(CoreConfig config) {
        HikariConfig hikari = new HikariConfig();
        DatabaseConfig databaseConfig = config.getDatabaseConfig();
        hikari.setJdbcUrl(databaseConfig.getJdbcUrl());
        hikari.setUsername(databaseConfig.getUser());
        hikari.setPassword(databaseConfig.getPassword());
        hikari.setMaximumPoolSize(databaseConfig.getMaximumPoolSize());

        // Connection pool settings
        hikari.setConnectionTimeout(30000); // 30 seconds
        hikari.setIdleTimeout(600000); // 10 minutes
        hikari.setMaxLifetime(1800000); // 30 minutes

        datasource = new HikariDataSource(hikari);

        // Run database migrations
        runMigrations(databaseConfig);
    }

    private void runMigrations(DatabaseConfig config) {
        Flyway flyway = Flyway.configure()
            .dataSource(config.getJdbcUrl(), config.getUser(), config.getPassword())
            .locations("classpath:db/migration")
            .load();

        flyway.migrate();
    }

    /**
     * Get a database connection from the pool.
     *
     * @return Connection from the pool
     * @throws SQLException if connection cannot be obtained
     */
    public Connection getConnection() throws SQLException {
        if (datasource == null) {
            throw new SQLException("DatabaseManager not initialized");
        }
        return datasource.getConnection();
    }

    /**
     * Close the connection pool and release all database connections.
     * Called automatically during shutdown.
     */
    @Override
    public void close() {
        if (datasource != null && !datasource.isClosed()) {
            datasource.close();
            datasource = null;
        }
    }

    /**
     * Check if the connection pool is active.
     *
     * @return true if datasource is initialized and not closed
     */
    public boolean isActive() {
        return datasource != null && !datasource.isClosed();
    }
}

