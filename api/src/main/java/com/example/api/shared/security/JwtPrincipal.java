package com.example.api.shared.security;

import com.example.api.user.domain.Role;

/**
 * Principal représentant l'utilisateur authentifié via JWT.
 * Contient les informations extraites du token pour éviter les requêtes DB.
 */
public record JwtPrincipal(
        Long userId,
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
