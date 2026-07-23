package com.colife.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.colife.api.auth.application.AuthUseCase;
import com.colife.api.auth.application.dto.MeResponseDto;
import com.colife.api.shared.exception.UnauthorizedException;
import com.colife.api.shared.security.JwtPrincipal;
import com.colife.api.user.domain.Role;
import com.colife.api.user.domain.User;
import com.colife.api.user.domain.UserRepositoryPort;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthUseCaseTest {

    @Mock
    private UserRepositoryPort userRepository;

    @InjectMocks
    private AuthUseCase authUseCase;

    @Test
    void should_return_me_with_full_profile_from_db() {
        // GIVEN
        UUID userId = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(userId, "alice@entreprise.com", Role.COLLABORATOR);

        User user = User.builder()
                .id(userId)
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@entreprise.com")
                .role(Role.COLLABORATOR)
                .bio("Ma bio")
                .phone("+33600000000")
                .address("Paris, France")
                .createdAt(java.time.LocalDate.of(2024, 10, 1))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // WHEN
        MeResponseDto result = authUseCase.getMe(principal);

        // THEN
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.firstName()).isEqualTo("Alice");
        assertThat(result.lastName()).isEqualTo("Smith");
        assertThat(result.email()).isEqualTo("alice@entreprise.com");
        assertThat(result.role()).isEqualTo(Role.COLLABORATOR);
        assertThat(result.bio()).isEqualTo("Ma bio");
        assertThat(result.phone()).isEqualTo("+33600000000");
        assertThat(result.address()).isEqualTo("Paris, France");
        verify(userRepository).findById(userId);
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void should_throw_unauthorized_when_user_absent() {
        // GIVEN
        UUID userId = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(userId, "ghost@entreprise.com", Role.COLLABORATOR);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThatThrownBy(() -> authUseCase.getMe(principal))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Utilisateur introuvable");
    }
}
