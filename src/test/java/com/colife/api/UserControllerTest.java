package com.colife.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.colife.api.shared.security.JwtPrincipal;
import com.colife.api.user.application.UserUseCase;
import com.colife.api.user.application.dto.UpdateProfileRequestDto;
import com.colife.api.user.application.dto.UserResponseDto;
import com.colife.api.user.domain.Role;
import com.colife.api.user.presentation.UserController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private UserUseCase userUseCase;

    @InjectMocks
    private UserController controller;

    private UserResponseDto sampleUser(UUID id, Role role) {
        return new UserResponseDto(id, "Jean", "Dupont", "jean@test.com",
                role, null, null, null, LocalDate.of(2025, 1, 1));
    }

    @Test
    void getAllUsers_should_return_200_with_list() {
        when(userUseCase.getAllUsers()).thenReturn(List.of(
                sampleUser(USER_ID, Role.COLLABORATOR),
                sampleUser(ADMIN_ID, Role.ADMIN)
        ));

        ResponseEntity<List<UserResponseDto>> response = controller.getAllUsers();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void countUsers_should_return_200_with_count() {
        when(userUseCase.countUsers()).thenReturn(42L);

        ResponseEntity<Map<String, Long>> response = controller.countUsers();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("count")).isEqualTo(42L);
    }

    @Test
    void getUserById_should_return_200_with_user() {
        when(userUseCase.getUserById(USER_ID)).thenReturn(sampleUser(USER_ID, Role.COLLABORATOR));

        ResponseEntity<UserResponseDto> response = controller.getUserById(USER_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(USER_ID);
        verify(userUseCase).getUserById(USER_ID);
    }

    @Test
    void updateProfile_should_return_200_with_updated_user() {
        UpdateProfileRequestDto request = new UpdateProfileRequestDto("Ma bio", "0601020304", "1 rue Test");
        when(userUseCase.updateProfile(USER_ID, request)).thenReturn(sampleUser(USER_ID, Role.COLLABORATOR));

        JwtPrincipal principal = new JwtPrincipal(USER_ID, "jean@test.com", Role.COLLABORATOR);
        ResponseEntity<UserResponseDto> response = controller.updateProfile(USER_ID, request, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        verify(userUseCase).updateProfile(USER_ID, request);
    }
}
