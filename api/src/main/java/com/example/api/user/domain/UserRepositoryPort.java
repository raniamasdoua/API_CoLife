package com.example.api.user.domain;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    List<User> findAll();
    User save(User user);
    User update(User user);
    void updatePassword(Long id, String encodedPassword);
    long countAll();
}
