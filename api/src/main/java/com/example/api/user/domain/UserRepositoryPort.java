package com.example.api.user.domain;

import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    User save(User user);
    User update(User user);

    /** Met à jour le rôle applicatif du miroir local (sync depuis Keycloak). */
    void updateRole(UUID id, Role role);
}
