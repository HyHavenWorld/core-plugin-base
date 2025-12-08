package com.hyhavenworld.core.domain;

import java.time.LocalDateTime;
import java.util.Set;

public class User{
    private String uuid;
    private String username;
    private LocalDateTime createdAt;
    private LocalDateTime lastSeen;
    private Long hoursPlayed;
    private Set<Role> roles;

    public User() {
    }

    public User(String uuid, String username, LocalDateTime createdAt, LocalDateTime lastSeen, Long hoursPlayed, Set<Role> roles) {
        this.uuid = uuid;
        this.username = username;
        this.createdAt = createdAt;
        this.lastSeen = lastSeen;
        this.hoursPlayed = hoursPlayed;
        this.roles = roles;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public Long getHoursPlayed() {
        return hoursPlayed;
    }

    public void setHoursPlayed(Long hoursPlayed) {
        this.hoursPlayed = hoursPlayed;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
}
