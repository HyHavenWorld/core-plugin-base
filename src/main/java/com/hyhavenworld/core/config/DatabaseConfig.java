package com.hyhavenworld.core.config;

public class DatabaseConfig {
    private final boolean enabled;
    private final String type;
    private final String host;
    private final int port;
    private final String user;
    private final String password;
    private final String databaseName;

    public DatabaseConfig(ConfigurationSection section) {
        if (section == null) section = new MemoryConfiguration();

        this.enabled = section.getBoolean("enabled", false);
        this.type = section.getString("type", "mysql");
        this.host = section.getString("host", "127.0.0.1");
        this.port = section.getInt("port", 3306);
        this.user = section.getString("user", "root");
        this.password = section.getString("password", "");
        this.databaseName = section.getString("name", "permissions_db");
    }

    public boolean isEnabled() { return enabled; }

    public String getJdbcUrl() {
        return "jdbc:" + type + "://" + host + ":" + port + "/" + databaseName;
    }

}
