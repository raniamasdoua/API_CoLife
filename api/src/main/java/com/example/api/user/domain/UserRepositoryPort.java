package com.example.api.user.domain;

import java.util.Optional;

public interface UserRepositoryPort {
        Optional<User> findById(Long id);
        Optional<User> findByEmail(String email);
}
