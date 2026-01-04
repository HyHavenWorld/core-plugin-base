CREATE TABLE IF NOT EXISTS user_permissions (
    user_uuid VARCHAR(36) NOT NULL,
    permission_node VARCHAR(255) NOT NULL,
    perm_value BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY(user_uuid, permission_node),
    FOREIGN KEY(user_uuid) REFERENCES users(uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_user_permissions_node ON user_permissions(permission_node);