package com.hyhavenworld.core.domain;

import java.time.LocalDateTime;
import java.util.Set;

public class Role {

    private Long id;
    private String name;
    private LocalDateTime createdAt;
    private Set<Permission> permissions;

    public Role() {
    }

    public Role(Long id, String name, LocalDateTime createdAt, Set<Permission> permissions) {
        this.id = id;
        this.createdAt = createdAt;
        this.name = name;
        this.permissions = permissions;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }
}
