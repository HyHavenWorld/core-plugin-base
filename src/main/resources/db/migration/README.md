# Database Migrations

This directory contains Flyway migration scripts organized by database engine.

## Structure

```
db/migration/
├── mysql/       MySQL-specific migrations
├── mariadb/     MariaDB-specific migrations
├── postgresql/  PostgreSQL-specific migrations
└── h2/          H2-specific migrations
```

## How It Works

The `DatabaseManager` automatically selects the correct migration directory based on the `database.type` configuration in `application.yml`.

For example:
- If `database.type: mysql`, migrations are loaded from `db/migration/mysql/`
- If `database.type: postgresql`, migrations are loaded from `db/migration/postgresql/`
- If `database.type: h2`, migrations are loaded from `db/migration/h2/`

## Migration Files

Each directory contains the same version numbers (V1-V6) with engine-specific SQL:

- **V1__create_users.sql** - Users table
- **V2__create_roles.sql** - Roles table
- **V3__create_role_inheritance.sql** - Role hierarchy
- **V4__create_user_roles.sql** - User-role junction table
- **V5__create_role_permissions.sql** - Role permissions
- **V6__create_user_permissions.sql** - User permissions

## Key Differences

### MySQL/MariaDB
- Uses `AUTO_INCREMENT` for primary keys
- Includes `ENGINE=InnoDB` specification
- Includes `CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci` for proper Unicode support

### PostgreSQL
- Uses `SERIAL` for auto-incrementing primary keys
- No engine or charset specifications (uses database defaults)
- Standard SQL syntax

### H2
- Uses `AUTO_INCREMENT` for primary keys (like MySQL)
- No engine or charset specifications
- Clean SQL syntax compatible with both MySQL and PostgreSQL
- Ideal for development and testing (supports embedded and in-memory modes)

## Adding New Migrations

When creating new migrations:

1. Create the migration file in **all four directories** with the same version number
2. Adapt the SQL syntax for each engine if needed
3. Test with all supported database engines

Example:
```
db/migration/mysql/V7__add_new_feature.sql
db/migration/mariadb/V7__add_new_feature.sql
db/migration/postgresql/V7__add_new_feature.sql
db/migration/h2/V7__add_new_feature.sql
```
