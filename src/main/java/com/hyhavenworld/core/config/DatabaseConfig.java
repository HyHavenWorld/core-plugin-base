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
    private final String driverClassName;

    public DatabaseConfig(Config config) {
        this.type = config.hasPath("database.type") ? config.getString("database.type") : "mysql";
        this.host = config.hasPath("database.host") ? config.getString("database.host") : "localhost";
        this.port = config.hasPath("database.port") ? config.getInt("database.port") : 3306;
        this.user = config.hasPath("database.user") ? config.getString("database.user") : "user";
        this.password = config.hasPath("database.pass") ? config.getString("database.pass") : "password";
        this.databaseName = config.hasPath("database.name") ? config.getString("database.name") : "databaseName";
        this.maximumPoolSize = config.hasPath("database.maximumPoolSize") ? config.getInt("database.maximumPoolSize") : 10;
        this.driverClassName = config.hasPath("database.driverClassName") ? config.getString("database.driverClassName") : "com.mysql.cj.jdbc.Driver";
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

    public String getDriverClassName() {
        return driverClassName;
    }
}
