package com.hyhavenworld.core.database;

import com.hyhavenworld.core.config.CoreConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {

    private HikariDataSource datasource;

    public void init(CoreConfig config) {
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(
                "jdbc:postgresql://" + config.getHost() + ":" + config.getPort() + "/" + config.getDbName()
        );
        hikari.setUsername(config.getUsername());
        hikari.setPassword(config.getPassword());
        hikari.setMaximumPoolSize(config.getPoolMaxSize());
        datasource = new HikariDataSource(hikari);

        // Initiate script create tables
        //runMigrations();
    }

    public Connection getConnection() throws SQLException {
        return datasource.getConnection();
    }
}
