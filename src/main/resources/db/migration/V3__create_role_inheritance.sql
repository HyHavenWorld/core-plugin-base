-- Role inheritance table
-- Allows a role to inherit permissions from one or more parent roles
-- Example: role 'moderator' can inherit from 'helper' and 'builder'
CREATE TABLE IF NOT EXISTS role_inheritance (
    parent_role_id INT NOT NULL,
    child_role_id INT NOT NULL,
    PRIMARY KEY(parent_role_id, child_role_id),
    FOREIGN KEY(parent_role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY(child_role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CHECK (parent_role_id != child_role_id)
);

CREATE INDEX idx_role_inheritance_child ON role_inheritance(child_role_id);