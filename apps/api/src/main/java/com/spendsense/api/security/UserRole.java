package com.spendsense.api.security;

public enum UserRole {
    USER,
    ADMIN,
    SUPPORT;

    public String authority() {
        return "ROLE_" + name();
    }
}
