CREATE TABLE IF NOT EXISTS user_roles (
    user_uuid VARCHAR(36) NOT NULL,
    role_id INT NOT NULL,
    PRIMARY KEY(user_uuid, role_id),
    FOREIGN KEY(user_uuid) REFERENCES users(uuid) ON DELETE CASCADE,
    FOREIGN KEY(role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_user_roles_role ON user_roles(role_id);