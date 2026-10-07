package com.supportrouter.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.supportrouter.Settings;

public record DatabaseConfig(String url, String username, String password) {
    public DatabaseConfig {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("url must not be blank");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        password = password == null ? "" : password;
    }

    public static DatabaseConfig fromEnvironment() {
        String url = Settings.get("DB_URL", "jdbc:mysql://localhost:3306/customer_support");
        String user = Settings.get("DB_USER", null);
        if (user == null || user.isBlank()) {
            throw new IllegalStateException("DB_USER must be set in .env or the environment");
        }
        return new DatabaseConfig(url, user, Settings.get("DB_PASSWORD", ""));
    }

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    public void checkConnection() {
        try (Connection ignored = connect()) {
            // Opening and closing a connection is the check.
        } catch (SQLException exception) {
            throw new DataAccessException("Cannot connect to MySQL at " + url + ": " + exception.getMessage()
                    + "\nIs the MySQL service running?", exception);
        }
    }

    @Override
    public String toString() {
        return "DatabaseConfig[url=" + url + ", username=" + username + "]";
    }
}
