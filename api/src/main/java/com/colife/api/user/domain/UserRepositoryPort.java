package com.colife.api.user.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    List<User> findAll();
    User save(User user);
    User update(User user);
    long countAll();

    /** Met à jour le rôle applicatif du miroir local (sync depuis Keycloak). */
    void updateRole(UUID id, Role role);
}
