package com.hyhavenworld.core.domain;

import java.util.Objects;

/**
 * Represents a permission node with a boolean value.
 * Similar to LuckPerms, a permission is a string key (node) with a true/false value.
 *
 * Examples:
 * - Permission("hyhavenworld.admin", true) - grants admin permission
 * - Permission("hyhavenworld.fly", false) - explicitly denies fly permission
 */
public class Permission {

    private String permissionNode;
    private boolean value;

    public Permission() {
    }

    public Permission(String permissionNode, boolean value) {
        this.permissionNode = permissionNode;
        this.value = value;
    }

    public String getPermissionNode() {
        return permissionNode;
    }

    public void setPermissionNode(String permissionNode) {
        this.permissionNode = permissionNode;
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Permission that = (Permission) o;
        return Objects.equals(permissionNode, that.permissionNode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permissionNode);
    }

    @Override
    public String toString() {
        return permissionNode + "=" + value;
    }
}