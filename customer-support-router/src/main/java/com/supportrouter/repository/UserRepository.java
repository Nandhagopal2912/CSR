package com.supportrouter.repository;

import java.util.List;
import java.util.Optional;

import com.supportrouter.model.Role;
import com.supportrouter.model.User;

public interface UserRepository {
    Optional<User> findByUsername(String username);

    List<User> findByRole(Role role);
}
