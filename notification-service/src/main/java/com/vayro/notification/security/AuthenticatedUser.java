package com.vayro.notification.security;

import java.util.Collection;
import java.util.List;

public class AuthenticatedUser {

    private final String userId;
    private final String email;
    private final String name;
    private final List<String> roles;

    public AuthenticatedUser(String userId, String email, String name, Collection<String> roles) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.roles = roles != null ? List.copyOf(roles) : List.of();
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public List<String> getRoles() {
        return roles;
    }

    public boolean isAdmin() {
        return roles.stream().anyMatch(r -> r.equalsIgnoreCase("ADMIN") || r.equalsIgnoreCase("ROLE_ADMIN"));
    }
}
