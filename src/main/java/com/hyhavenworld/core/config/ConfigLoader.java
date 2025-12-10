package com.hyhavenworld.core.config;

public class ConfigLoader {

    private CoreConfig coreConfig;
    private StorageConfig storageConfig;
    private Database database;
    private UserRepository userRepo;
    private RoleRepository roleRepo;
    private PermissionRepository permissionRepo;


    public void init() {

        this.coreConfig = new CoreConfig(configFile);

        // 2. Crear StorageConfig
        this.storageConfig = new StorageConfig(coreConfig.getStorageType());

        // 3. Elegir backend según la config
        if (coreConfig.isDatabaseEnabled()) {
            this.database = new HikariDatabase(coreConfig.getDatabaseConfig());
            database.connect();
            flywayMigrations(database);

            this.userRepo = new UserRepositoryDb(database);
            this.roleRepo = new RoleRepositoryDb(database);
            this.permissionRepo = new PermissionRepositoryDb(database);

        } else {
            File file = new File(coreConfig.getFilePath());

            this.userRepo = new UserRepositoryFile(file);
            this.roleRepo = new RoleRepositoryFile(file);
            this.permissionRepo = new PermissionRepositoryFile(file);
        }

        // 4. Inicializar servicios (RoleService, PermissionService, etc)
    }
}
