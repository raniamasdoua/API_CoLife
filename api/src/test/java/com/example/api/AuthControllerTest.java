package com.example.api;

import com.example.api.auth.application.AuthUseCase;
import com.example.api.auth.application.dto.LoginRequestDto;
import com.example.api.auth.application.dto.LoginResponseDto;
import com.example.api.auth.application.dto.MeResponseDto;
import com.example.api.auth.application.dto.RegisterRequestDto;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthUseCase authUseCase;

    @InjectMocks
    private AuthController authController;

    @Test
    void should_register_and_return_200() {

        // GIVEN
        RegisterRequestDto requestDto = new RegisterRequestDto(
                "Alice", "Smith",
                "alice.smith@entreprise.com",
                "Password123!@#",
                null, null, null
        );

        // WHEN
        ResponseEntity<Void> response = authController.register(requestDto);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNull();
        verify(authUseCase).register(requestDto);
    }

    @Test
    void should_delegate_register_to_use_case() {

        // GIVEN
        RegisterRequestDto requestDto = new RegisterRequestDto(
                "Bob", "Martin",
                "bob.martin@entreprise.com",
                "SecurePass123!@#",
                null, null, null
        );

        // WHEN
        authController.register(requestDto);

        // THEN
        verify(authUseCase, times(1)).register(requestDto);
    }

    @Test
    void should_login_and_return_token_with_bearer_type() {

        // GIVEN
        LoginRequestDto requestDto = new LoginRequestDto(
                "alice.smith@entreprise.com",
                "Password123!@#"
        );
        LoginResponseDto expectedResponse = new LoginResponseDto("jwt-token", "Bearer");
        when(authUseCase.login(requestDto)).thenReturn(expectedResponse);

        // WHEN
        ResponseEntity<LoginResponseDto> response = authController.login(requestDto);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isEqualTo("jwt-token");
        assertThat(response.getBody().type()).isEqualTo("Bearer");
        verify(authUseCase).login(requestDto);
    }

    @Test
    void should_return_me_when_authenticated_with_jwt_principal() {

        // GIVEN
        JwtPrincipal principal = new JwtPrincipal(1L, "alice@entreprise.com", Role.COLLABORATOR);
        MeResponseDto expectedMe = new MeResponseDto(
                1L, "Alice", "Smith", "alice@entreprise.com", Role.COLLABORATOR,
                "Ma bio", "+33600000000", "Paris, France",
                java.time.LocalDate.of(2024, 10, 1));
        when(authUseCase.getMe(principal)).thenReturn(expectedMe);

        // WHEN
        ResponseEntity<MeResponseDto> response = authController.me(principal);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(1L);
        assertThat(response.getBody().firstName()).isEqualTo("Alice");
        assertThat(response.getBody().email()).isEqualTo("alice@entreprise.com");
        assertThat(response.getBody().role()).isEqualTo(Role.COLLABORATOR);
        verify(authUseCase).getMe(principal);
    }
}
