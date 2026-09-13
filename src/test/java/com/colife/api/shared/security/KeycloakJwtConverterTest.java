package com.colife.api.shared.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import com.colife.api.user.domain.Role;
import com.colife.api.user.domain.User;
import com.colife.api.user.domain.UserRepositoryPort;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeycloakJwtConverterTest {

    @Mock
    private UserRepositoryPort userRepository;

    private KeycloakJwtConverter converter;

    private static final UUID USER_ID = UUID.randomUUID();

    private Jwt buildJwt(Role realmRole) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(USER_ID.toString())
                .claim("email", "ana@entreprise.com")
                .claim("given_name", "Ana")
                .claim("family_name", "Test")
                .claim("realm_access", Map.of("roles", List.of(realmRole.name())))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .build();
    }

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        converter = new KeycloakJwtConverter(userRepository);
    }

    @Test
    void provisionne_le_profil_local_a_la_premiere_connexion() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        AbstractAuthenticationToken result = converter.convert(buildJwt(Role.COLLABORATOR));

        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void ignore_silencieusement_une_violation_de_cle_deja_existante() {
        // Simule la course : plusieurs requêtes concurrentes passent toutes par ce
        // convertisseur à la toute première connexion, aucune ne trouve encore de ligne
        // locale, et une seule gagne la course d'insertion — les autres doivent pouvoir
        // continuer normalement plutôt que de faire échouer l'authentification.
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint \"users_pkey\""));

        AbstractAuthenticationToken result = converter.convert(buildJwt(Role.COLLABORATOR));

        assertThat(result).isNotNull();
        assertThat(result.isAuthenticated()).isTrue();
    }

    @Test
    void ne_touche_pas_au_profil_existant_si_le_role_n_a_pas_change() {
        User existing = User.builder().id(USER_ID).role(Role.COLLABORATOR).build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));

        converter.convert(buildJwt(Role.COLLABORATOR));

        verify(userRepository, never()).save(any());
        verify(userRepository, never()).updateRole(any(), any());
    }

    @Test
    void synchronise_le_role_local_si_keycloak_a_promu_l_utilisateur() {
        User existing = User.builder().id(USER_ID).role(Role.COLLABORATOR).build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));

        converter.convert(buildJwt(Role.ADMIN));

        verify(userRepository).updateRole(USER_ID, Role.ADMIN);
    }
}
