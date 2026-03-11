package com.example.api;

import com.example.api.auth.application.AuthUseCase;
import com.example.api.auth.application.dto.LoginRequestDto;
import com.example.api.auth.application.dto.LoginResponseDto;
import com.example.api.auth.application.dto.MeResponseDto;
import com.example.api.auth.application.dto.RegisterRequestDto;
import com.example.api.shared.exception.ConflictException;
import com.example.api.shared.exception.UnauthorizedException;
import com.example.api.shared.security.JwtPrincipal;
import com.example.api.shared.security.JwtService;
import com.example.api.user.domain.Role;
import com.example.api.user.domain.User;
import com.example.api.user.domain.UserRepositoryPort;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthUseCaseTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthUseCase authUseCase;

    @Test
    void should_register_user_successfully() {

        // GIVEN
        RegisterRequestDto requestDto = new RegisterRequestDto(
                "Alice",
                "Smith",
                "alice.smith@entreprise.com",
                "Password123!@#"
        );

        when(passwordEncoder.encode(anyString()))
                .thenReturn("hashed-password");

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.empty());

        // WHEN
        authUseCase.register(requestDto);

        // THEN
        verify(userRepository).save(argThat(user ->
                user.getFirstName().equals("Alice")
                        && user.getLastName().equals("Smith")
                        && user.getEmail().equals(requestDto.email())
                        && user.getPassword().equals("hashed-password")
                        && user.getRole() == Role.COLLABORATOR
        ));
    }

    @Test
    void should_throw_conflict_when_email_exists() {

        // GIVEN
        RegisterRequestDto requestDto = new RegisterRequestDto(
                "Alice",
                "Smith",
                "alice.smith@entreprise.com",
                "Password123!@#"
        );

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(mock(User.class)));

        // WHEN / THEN
        assertThatThrownBy(() ->
                authUseCase.register(requestDto)
        )
                .isInstanceOf(ConflictException.class)
                .hasMessage("Email déjà utilisé");

        verify(userRepository, never()).save(any());
    }

    @Test
    void should_return_token_when_credentials_ok() {

        // GIVEN
        LoginRequestDto requestDto = new LoginRequestDto(
                "alice.smith@entreprise.com",
                "Password123!@#"
        );

        User user = User.builder()
                .id(1L)
                .email("alice.smith@entreprise.com")
                .password("hashed-password")
                .role(Role.COLLABORATOR)
                .build();

        when(userRepository.findByEmail("alice.smith@entreprise.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(requestDto.password(), user.getPassword()))
                .thenReturn(true);
        when(jwtService.generateToken(user.getId(), user.getEmail(), Role.COLLABORATOR))
                .thenReturn("jwt-access-token");

        // WHEN
        LoginResponseDto result = authUseCase.login(requestDto);

        // THEN
        assertThat(result.accessToken()).isEqualTo("jwt-access-token");
        assertThat(result.type()).isEqualTo("Bearer");
        verify(jwtService).generateToken(eq(user.getId()), eq(user.getEmail()), eq(Role.COLLABORATOR));
    }

    @Test
    void should_throw_unauthorized_when_credentials_invalid() {

        // GIVEN
        LoginRequestDto requestDto = new LoginRequestDto(
                "alice.smith@entreprise.com",
                "WrongPassword123!@#"
        );

        User user = User.builder()
                .id(1L)
                .email("alice.smith@entreprise.com")
                .password("hashed-password")
                .role(Role.COLLABORATOR)
                .build();

        when(userRepository.findByEmail("alice.smith@entreprise.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(requestDto.password(), user.getPassword()))
                .thenReturn(false);

        // WHEN / THEN
        assertThatThrownBy(() -> authUseCase.login(requestDto))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Identifiants invalides");

        verify(jwtService, never()).generateToken(anyLong(), anyString(), any(Role.class));
    }

    @Test
    void should_throw_unauthorized_when_user_absent() {

        // GIVEN
        LoginRequestDto requestDto = new LoginRequestDto(
                "unknown@entreprise.com",
                "Password123!@#"
        );

        when(userRepository.findByEmail("unknown@entreprise.com"))
                .thenReturn(Optional.empty());

        // WHEN / THEN
        assertThatThrownBy(() -> authUseCase.login(requestDto))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Identifiants invalides");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(anyLong(), anyString(), any(Role.class));
    }

    @Test
    void should_return_me_with_full_profile_from_db() {
        // GIVEN
        JwtPrincipal principal = new JwtPrincipal(1L, "alice@entreprise.com", Role.COLLABORATOR);

        User user = User.builder()
                .id(1L)
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@entreprise.com")
                .role(Role.COLLABORATOR)
                .bio("Ma bio")
                .phone("+33600000000")
                .address("Paris, France")
                .createdAt(java.time.LocalDate.of(2024, 10, 1))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // WHEN
        MeResponseDto result = authUseCase.getMe(principal);

        // THEN
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.firstName()).isEqualTo("Alice");
        assertThat(result.lastName()).isEqualTo("Smith");
        assertThat(result.email()).isEqualTo("alice@entreprise.com");
        assertThat(result.role()).isEqualTo(Role.COLLABORATOR);
        assertThat(result.bio()).isEqualTo("Ma bio");
        assertThat(result.phone()).isEqualTo("+33600000000");
        assertThat(result.address()).isEqualTo("Paris, France");
        verify(userRepository).findById(1L);
        verify(userRepository, never()).findByEmail(anyString());
    }
}
