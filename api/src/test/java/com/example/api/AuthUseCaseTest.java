package com.example.api;

import com.example.api.auth.application.AuthUseCase;
import com.example.api.auth.application.dto.RegisterRequestDto;
import com.example.api.user.domain.Role;
import com.example.api.user.domain.User;
import com.example.api.user.domain.UserRepositoryPort;
import com.example.api.shared.exception.ConflictException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthUseCaseTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthUseCase authUseCase;

    @Test
    void should_register_user_successfully() {

        // GIVEN
        RegisterRequestDto requestDto = new RegisterRequestDto(
                "Alice",
                "Smith",
                "alice.smith@company.com",
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
                "alice.smith@company.com",
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
}
