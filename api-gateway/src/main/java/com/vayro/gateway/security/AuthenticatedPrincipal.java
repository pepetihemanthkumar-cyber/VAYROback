package com.vayro.gateway.security;

import java.security.Principal;

public class AuthenticatedPrincipal implements Principal {

    private final String userId;
    private final String email;
    private final String name;
    private final String role;

    public AuthenticatedPrincipal(String userId, String email, String name, String role) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.role = role;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String getName() {
        return email;
    }

    public String getDisplayName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    @Override
    public String toString() {
        return "AuthenticatedPrincipal{" +
                "userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
