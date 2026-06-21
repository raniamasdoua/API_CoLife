package com.example.api.shared.security;

import com.example.api.user.domain.Role;

import java.util.UUID;

/**
 * Principal représentant l'utilisateur authentifié via OIDC (Keycloak).
 * Le {@code userId} est le sub Keycloak (UUID), extrait directement du token.
 */
public record JwtPrincipal(
        UUID userId,
        String email,
        Role role
) {
    public boolean hasRole(Role userRole) {
        return role != null && role.equals(userRole);
    }

    public boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }
}
