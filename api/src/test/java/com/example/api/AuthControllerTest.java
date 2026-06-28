package com.example.api;

import com.example.api.auth.application.AuthUseCase;
import com.example.api.auth.application.dto.MeResponseDto;
import com.example.api.auth.presentation.AuthController;
import com.example.api.shared.security.JwtPrincipal;
import com.example.api.user.domain.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthUseCase authUseCase;

    @InjectMocks
    private AuthController authController;

    @Test
    void should_return_me_when_authenticated_with_jwt_principal() {

        // GIVEN
        UUID userId = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(userId, "alice@entreprise.com", Role.COLLABORATOR);
        MeResponseDto expectedMe = new MeResponseDto(
                userId, "Alice", "Smith", "alice@entreprise.com", Role.COLLABORATOR,
                "Ma bio", "+33600000000", "Paris, France",
                java.time.LocalDate.of(2024, 10, 1));
        when(authUseCase.getMe(principal)).thenReturn(expectedMe);

        // WHEN
        ResponseEntity<MeResponseDto> response = authController.me(principal);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(userId);
        assertThat(response.getBody().firstName()).isEqualTo("Alice");
        assertThat(response.getBody().email()).isEqualTo("alice@entreprise.com");
        assertThat(response.getBody().role()).isEqualTo(Role.COLLABORATOR);
        verify(authUseCase).getMe(principal);
    }
}
