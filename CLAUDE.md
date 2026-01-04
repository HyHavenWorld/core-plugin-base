# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`core-plugin-base` is a foundational library for Hytale plugins, providing common infrastructure for user management, roles, and permissions. The library is designed to work independently of the Hytale API and supports two storage backends: database (PostgreSQL via JDBC) or file-based (YAML).

**Key Technologies:**
- Java 25
- Maven build system
- PostgreSQL database (with HikariCP connection pooling)
- Flyway for database migrations
- Typesafe Config for configuration management
- SnakeYAML for file-based storage

## Build Commands

```bash
# Compile the project
mvn compile

# Package into JAR
mvn package

# Clean build artifacts
mvn clean

# Install to local Maven repository
mvn install
```

## Architecture Overview

### Initialization Sequence

The library uses a single-step initialization via the **CorePluginManager** facade:

1. **Initialize CorePluginManager**: `CorePluginManager.initialize()`
   - Automatically initializes storage (database or file) based on configuration
   - Creates service instances (UserService, RoleService, PermissionService)
   - Runs database migrations if using database storage

2. **Shutdown**: `CorePluginManager.shutdown()`
   - Closes database connection pool (if using database storage)
   - Releases all resources
   - Should be called in plugin's onDisable()

### Configuration System

Configuration is loaded from `application.yml` in the working directory via `CoreConfig`:

- **storageType**: `DATABASE` or `FILE` - determines which repository implementation to use
- **database**: Connection settings (host, port, name, user, pass)
- **file.path**: Path to YAML file for file-based storage

The `DatabaseConfig` class constructs JDBC URLs and defaults to PostgreSQL on port 5254.

### Service Layer Architecture

The codebase follows a layered architecture with a Facade pattern:

```
CorePluginManager (Facade + Singleton)
    ↓
ServiceRegistry (internal, manages services)
    ↓
Service Layer (UserService, RoleService, PermissionService)
    ↓
Repository Interface (UserRepository, RoleRepository)
    ↓
Repository Implementations (ImplJDBC or ImplFile - selected via switch on StorageType)
    ↓
Storage Layer (StorageManager → DatabaseManager + HikariCP or YamlDataStore)
```

**Pattern Details:**
- **CorePluginManager**: Facade that provides simple API for plugin developers. Handles initialization and shutdown
- **ServiceRegistry**: Internal class that manages service instances. Not directly accessed by plugins
- **StorageManager**: Manages database connections when using DATABASE storage. Auto-closed on shutdown
- **DatabaseManager**: Manages HikariCP connection pool and Flyway migrations. Implements AutoCloseable
- **YamlDataStore**: Thread-safe YAML file access with read/write locks. Auto-reloads on each operation
- Services use switch expressions to select repository implementation based on `CoreConfig.getStorageType()`
- Repository implementations are instantiated directly in service constructors (no DI framework)

### Storage Abstraction

Each repository has two implementations selected at runtime:

- **JDBC Implementation** (`*ImplJDBC`): Uses `StorageManager.get().getDatabase().getConnection()` to access HikariCP connection pool
  - Production-ready with connection pooling and transactions
  - Supports complex queries and joins
  - Uses Flyway for schema migrations

- **File Implementation** (`*ImplFile`): Uses `YamlDataStore` for thread-safe YAML file access
  - Thread-safe with read/write locks
  - Auto-reloads file on each operation for multi-instance consistency
  - Suitable for small deployments or development
  - Stores all data in a single YAML file with structure: users, roles, metadata

**Note**: Both implementations are fully functional. Choose DATABASE for production with many users, or FILE for simple deployments.

### Domain Model

Core entities:
- **User**: UUID-based, tracks username, creation time, last seen, hours played, assigned roles, and direct permissions
- **Role**: ID-based, contains name and set of permissions
- **Permission**: Value object with permissionNode (String) and value (boolean), similar to LuckPerms

Relationships are managed through junction tables (user_roles, role_inheritance, role_permissions, user_permissions).

### Database Migrations

Flyway manages schema via `src/main/resources/db/migration/`:
- V1: users table
- V2: roles table
- V3: role_inheritance (role hierarchy)
- V4: user_roles (many-to-many user-role relationship)
- V5: role_permissions (permission nodes assigned to roles)
- V6: user_permissions (permission nodes assigned directly to users)

Migrations run automatically when `DatabaseManager.init()` is called.

**Permission System:**
- Permissions are stored as strings (permission nodes) like "hyhavenworld.admin", "hyhavenworld.fly"
- Each permission has a boolean value (true = granted, false = denied)
- User permissions override role permissions
- Roles can inherit from parent roles recursively

## Usage Pattern

**Recommended API (CorePluginManager):**

```java
// In plugin onEnable()
public void onEnable() {
    // Initialize with default config (reads from application.yml)
    CorePluginManager.initialize();

    // Or with custom config
    // CoreConfig config = new CoreConfig();
    // CorePluginManager.initialize(config);
}

// Accessing services anywhere in your plugin
UserService users = CorePluginManager.get().users();
RoleService roles = CorePluginManager.get().roles();
PermissionService perms = CorePluginManager.get().permissions();

// Example usage
Optional<User> user = users.getUser(uuid);
users.addRole(uuid, "admin");
boolean hasPermission = perms.hasPermission(uuid, "hyhavenworld.admin");

// In plugin onDisable()
public void onDisable() {
    // Cleanup all resources (close database connections, etc.)
    CorePluginManager.shutdown();
}
```

**Legacy API (still supported but not recommended):**

```java
// Initialization (once at startup)
ServiceRegistry.init();
StorageManager.init(new CoreConfig()); // Only if using database storage

// Accessing services
UserService users = ServiceRegistry.get().users();
RoleService roles = ServiceRegistry.get().roles();
PermissionService perms = ServiceRegistry.get().permissions();
```

## Development Notes

- **Configuration Loading**: `CoreConfig` uses Typesafe Config to parse `application.yml` (not standard YAML parsing)
- **Connection Pooling**: HikariCP is initialized in `DatabaseManager.init()` and provides thread-safe connection access with configurable pool settings
- **Error Handling**: Custom exceptions `CorePersistenceException` and `RegistryException` for persistence and registry errors
- **Resource Management**: All resources (database connections, file handles) are properly closed via AutoCloseable implementations
- **Thread Safety**: YamlDataStore uses ReadWriteLock for thread-safe file operations. DatabaseManager uses HikariCP which is thread-safe
- **Testing**: Both JDBC and File implementations have comprehensive test coverage using H2 in-memory database and temporary files