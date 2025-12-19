-- User permissions table
-- Stores permission nodes directly assigned to users (not through roles)
-- These permissions override role permissions
-- Permission nodes are strings (e.g., "hyhavenworld.admin", "hyhavenworld.fly")
-- Value indicates if permission is granted (true) or denied (false)
CREATE TABLE IF NOT EXISTS user_permissions (
    user_uuid VARCHAR(36) NOT NULL,
    permission_node VARCHAR(255) NOT NULL,
    value BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY(user_uuid, permission_node),
    FOREIGN KEY(user_uuid) REFERENCES users(uuid) ON DELETE CASCADE
);

CREATE INDEX idx_user_permissions_node ON user_permissions(permission_node);