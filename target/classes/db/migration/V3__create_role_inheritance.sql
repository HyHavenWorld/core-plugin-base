CREATE TABLE IF NOT EXISTS role_inheritance (
    parent_role_id INT NOT NULL,
    child_role_id INT NOT NULL,
    PRIMARY KEY(parent_role_id, child_role_id),
    FOREIGN KEY(parent_role_id) REFERENCES roles(id),
    FOREIGN KEY(child_role_id) REFERENCES roles(id)
);