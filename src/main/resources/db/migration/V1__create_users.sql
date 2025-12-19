CREATE TABLE IF NOT EXISTS users (
    uuid VARCHAR(36) NOT NULL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_seen TIMESTAMP,
    hours_played BIGINT DEFAULT 0
);

CREATE INDEX idx_users_username ON users(username);