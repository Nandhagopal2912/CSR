package com.supportrouter.model;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;

public class Customer {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final String customerId;
    private final String name;
    private final String email;

    public Customer(String customerId, String name, String email) {
        this.customerId = requireText(customerId, "customerId");
        this.name = requireText(name, "name");
        this.email = requireText(email, "email");
    }

    // A blank ID is derived from the email, so the same customer always gets the same ID.
    public static Customer withOptionalId(String customerId, String name, String email) {
        String id = customerId == null || customerId.isBlank() ? idForEmail(requireText(email, "email"))
                : customerId.trim();
        return new Customer(id, requireText(name, "name").trim(), email.trim());
    }

    public static String idForEmail(String email) {
        byte[] normalized = email.trim().toLowerCase().getBytes(StandardCharsets.UTF_8);
        return "C-" + UUID.nameUUIDFromBytes(normalized);
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL.matcher(email.trim()).matches();
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
