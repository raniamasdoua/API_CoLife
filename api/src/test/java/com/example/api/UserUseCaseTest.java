package com.example.api;

import com.example.api.shared.exception.ResourceNotFoundException;
import com.example.api.user.application.UserUseCase;
import com.example.api.user.application.dto.UserResponseDto;
import com.example.api.user.domain.Role;
import com.example.api.user.domain.User;
import com.example.api.user.domain.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserUseCaseTest {

    @Mock
    private UserRepositoryPort userRepository;

    @InjectMocks
    private UserUseCase userUseCase;

    @Test
    void should_return_user_when_found() {
        // GIVEN
        User targetUser = User.builder().id(2L).email("bob@entreprise.com").role(Role.COLLABORATOR).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));

        // WHEN
        UserResponseDto result = userUseCase.getUserById(2L);

        // THEN
        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.email()).isEqualTo("bob@entreprise.com");
        assertThat(result.role()).isEqualTo(Role.COLLABORATOR);
    }

    @Test
    void should_return_own_profile_when_found() {
        // GIVEN
        User currentUser = User.builder().id(1L).email("alice@entreprise.com").role(Role.COLLABORATOR).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));

        // WHEN
        UserResponseDto result = userUseCase.getUserById(1L);

        // THEN
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.email()).isEqualTo("alice@entreprise.com");
    }

    @Test
    void should_throw_not_found_when_user_id_does_not_exist() {
        // GIVEN
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThatThrownBy(() -> userUseCase.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Utilisateur non trouvé");
    }
}
