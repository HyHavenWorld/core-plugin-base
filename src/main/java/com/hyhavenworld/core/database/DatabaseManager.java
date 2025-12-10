package com.hyhavenworld.core.database;

import com.hyhavenworld.core.config.CoreConfig;
import com.hyhavenworld.core.config.DatabaseConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {


    private HikariDataSource datasource;

    public void init(CoreConfig config) {
        HikariConfig hikari = new HikariConfig();
        DatabaseConfig databaseConfig = config.getDatabaseConfig();
        hikari.setJdbcUrl(databaseConfig.getJdbcUrl());
        hikari.setUsername(databaseConfig.getUser());
        hikari.setPassword(databaseConfig.getPassword());
        hikari.setMaximumPoolSize(databaseConfig.getMaximumPoolSize());
        datasource = new HikariDataSource(hikari);
    }

    public Connection getConnection() throws SQLException {
        return datasource.getConnection();
    }

    public void close() {
        if (datasource != null) datasource.close();
    }
}
