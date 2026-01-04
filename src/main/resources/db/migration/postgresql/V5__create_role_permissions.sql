CREATE TABLE IF NOT EXISTS role_permissions (
    role_id INT NOT NULL,
    permission_node VARCHAR(255) NOT NULL,
    perm_value BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY(role_id, permission_node),
    FOREIGN KEY(role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE INDEX idx_role_permissions_node ON role_permissions(permission_node);
