package com.supportrouter.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.supportrouter.model.Role;
import com.supportrouter.model.User;

public class MySqlUserRepository implements UserRepository {
    private final DatabaseConfig config;

    public MySqlUserRepository(DatabaseConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config is required");
        }
        this.config = config;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT username, name, email, role FROM users WHERE username = ?";
        try (Connection connection = config.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapUser(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to find user: " + exception.getMessage(), exception);
        }
    }

    @Override
    public List<User> findByRole(Role role) {
        String sql = "SELECT username, name, email, role FROM users WHERE role = ? ORDER BY name";
        List<User> users = new ArrayList<>();
        try (Connection connection = config.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, role.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    users.add(mapUser(resultSet));
                }
            }
            return users;
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to find users: " + exception.getMessage(), exception);
        }
    }

    private static User mapUser(ResultSet resultSet) throws SQLException {
        return new User(resultSet.getString("username"), resultSet.getString("name"),
                resultSet.getString("email"), Role.valueOf(resultSet.getString("role")));
    }
}
