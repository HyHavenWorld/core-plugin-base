package com.hyhavenworld.core.config;

import com.typesafe.config.Config;

public class DatabaseConfig {
    private final String type;
    private final String host;
    private final int port;
    private final String user;
    private final String password;
    private final String databaseName;
    private final int maximumPoolSize;

    public DatabaseConfig(Config config) {
        this.type = config.hasPath("database.type") ? config.getString("database.type") : "postgre";
        this.host = config.hasPath("database.host") ? config.getString("database.host") : "localhost";
        this.port = config.hasPath("database.port") ? config.getInt("database.port") : 5254;
        this.user = config.hasPath("database.user") ? config.getString("database.user") : "user";
        this.password = config.hasPath("database.password") ? config.getString("database.password") : "password";
        this.databaseName = config.hasPath("database.databaseName") ? config.getString("database.databaseName") : "databaseName";
        this.maximumPoolSize = config.hasPath("database.maximumPoolSize") ? config.getInt("database.maximumPoolSize") : 10;
    }

    public String getJdbcUrl() {
        return "jdbc:" + type + "://" + host + ":" + port + "/" + databaseName;
    }

    public String getPassword() {
        return password;
    }

    public String getUser() {
        return user;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }
}
