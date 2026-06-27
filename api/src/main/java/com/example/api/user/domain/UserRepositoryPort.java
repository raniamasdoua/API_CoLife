package com.example.api.user.domain;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    Optional<User> findByResetToken(String resetToken);
    List<User> findAll();
    User save(User user);
    User update(User user);
    void updatePassword(Long id, String encodedPassword);
    void updateResetToken(Long id, String resetToken, java.time.LocalDateTime resetTokenExpiry);
    long countAll();
}
