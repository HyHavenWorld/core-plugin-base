CREATE TABLE IF NOT EXISTS permissions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    role_id INT NOT NULL,
    permission_key VARCHAR(100) NOT NULL,
    value TINYINT(1) NOT NULL DEFAULT 1,
    FOREIGN KEY(role_id) REFERENCES roles(id)
);