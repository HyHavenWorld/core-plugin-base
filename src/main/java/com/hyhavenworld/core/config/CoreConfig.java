package com.hyhavenworld.core.config;

public class CoreConfig {

    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private int poolMaxSize;

    public CoreConfig(String host, int port, String database, String username, String password, int poolMaxSize) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.poolMaxSize = poolMaxSize;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getPoolMaxSize() {
        return poolMaxSize;
    }
}
