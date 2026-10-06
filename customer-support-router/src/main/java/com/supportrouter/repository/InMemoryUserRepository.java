package com.supportrouter.repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.supportrouter.model.Role;
import com.supportrouter.model.User;

public class InMemoryUserRepository implements UserRepository {
    private final Map<String, User> users = new LinkedHashMap<>();

    public InMemoryUserRepository(List<User> users) {
        users.forEach(user -> this.users.put(user.username(), user));
    }

    public static InMemoryUserRepository withDemoUsers() {
        return new InMemoryUserRepository(List.of(
                new User("priya", "Priya Sharma", "priya@example.com", Role.CUSTOMER),
                new User("arjun", "Arjun Kumar", "arjun@example.com", Role.CUSTOMER),
                new User("ravi", "Ravi Shankar", "ravi@support.example.com", Role.AGENT),
                new User("meena", "Meena Iyer", "meena@support.example.com", Role.AGENT),
                new User("sanjay", "Sanjay Rao", "sanjay@support.example.com", Role.SUPERVISOR)));
    }

    @Override
    public synchronized Optional<User> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    @Override
    public synchronized List<User> findByRole(Role role) {
        return users.values().stream().filter(user -> user.role() == role).toList();
    }
}
