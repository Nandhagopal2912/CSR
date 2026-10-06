package com.supportrouter.model;

public record User(String username, String name, String email, Role role) {
    public User {
        if (username == null || username.isBlank() || name == null || name.isBlank()
                || email == null || email.isBlank() || role == null) {
            throw new IllegalArgumentException("username, name, email, and role are required");
        }
    }

    public boolean can(Permission permission) {
        return role.allows(permission);
    }

    public Customer asCustomer() {
        return new Customer(username, name, email);
    }
}
