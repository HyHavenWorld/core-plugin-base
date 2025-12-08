package com.hyhavenworld.core.domain;

import java.time.LocalDateTime;

public class Permission {

    private Long id;
    private String name;
    private LocalDateTime
            createdAt;
    public Permission() {
    }

    public Permission(Long id, String name, LocalDateTime createdAt) {
        this.id = id;
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
}
