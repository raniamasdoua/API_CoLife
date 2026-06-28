package com.example.api.auth.application;

import com.example.api.auth.application.dto.MeResponseDto;
import com.example.api.shared.exception.UnauthorizedException;
import com.example.api.shared.security.JwtPrincipal;
import com.example.api.user.domain.User;
import com.example.api.user.domain.UserRepositoryPort;
import org.springframework.stereotype.Service;

/**
 * Cas d'usage d'authentification.
 *
 * <p>L'inscription, la connexion et la réinitialisation de mot de passe sont désormais
 * gérées par Keycloak (OIDC). Le backend, en tant que resource server, expose uniquement
 * la consultation du profil de l'utilisateur courant.</p>
 */
@Service
public class AuthUseCase {

    private final UserRepositoryPort userRepository;

    public AuthUseCase(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Retourne les informations complètes de l'utilisateur connecté (profil local
     * provisionné depuis les claims Keycloak).
     */
    public MeResponseDto getMe(JwtPrincipal principal) {
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new UnauthorizedException("Utilisateur introuvable"));
        return new MeResponseDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getBio(),
                user.getPhone(),
                user.getAddress(),
                user.getCreatedAt()
        );
    }
}
