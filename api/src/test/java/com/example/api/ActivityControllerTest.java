package com.example.api;

import com.example.api.activity.application.ActivityUseCase;
import com.example.api.activity.application.dto.ActivityResponseDto;
import com.example.api.activity.application.dto.ActivityTypeDto;
import com.example.api.activity.application.dto.CreateActivityRequestDto;
import com.example.api.activity.application.dto.LocationDto;
import com.example.api.activity.application.dto.UpdateActivityRequestDto;
import com.example.api.activity.presentation.ActivityController;
import com.example.api.shared.security.JwtPrincipal;
import com.example.api.user.domain.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityControllerTest {

    @Mock
    private ActivityUseCase activityUseCase;

    @InjectMocks
    private ActivityController activityController;

    @Test
    void should_return_201_created() {
        JwtPrincipal principal = new JwtPrincipal(3L, "u@entreprise.com", Role.COLLABORATOR);
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
                "Atelier",
                null,
                1L,
                LocalDate.of(2026, 5, 1),
                LocalTime.of(15, 0),
                LocalTime.of(17, 0),
                12,
                new LocationDto("5 av", "Bât B", "69000", "Lyon"));

        ActivityResponseDto responseDto = new ActivityResponseDto(
                42L,
                "Atelier",
                null,
                12,
                1,
                new LocationDto("5 av", "Bât B", "69000", "Lyon"),
                new ActivityTypeDto(1L, "Culture"),
                LocalDate.of(2026, 5, 1),
                LocalTime.of(15, 0),
                LocalTime.of(17, 0),
                "Organisateur Test");

        when(activityUseCase.create(eq(3L), any(CreateActivityRequestDto.class))).thenReturn(responseDto);

        ResponseEntity<ActivityResponseDto> response = activityController.create(principal, dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(42L);
        verify(activityUseCase).create(3L, dto);
    }

    // ─── Tests : PUT /activities/{id} ───────────────────────────────────────

    private ActivityResponseDto sampleResponse() {
        return new ActivityResponseDto(
                42L, "Titre modifié", "Desc", 10, 1,
                new LocationDto("1 rue A", null, "75001", "Paris"),
                new ActivityTypeDto(1L, "Sport"),
                LocalDate.of(2026, 5, 10),
                LocalTime.of(14, 0), LocalTime.of(16, 0),
                "Organisateur Test");
    }

    private UpdateActivityRequestDto updateDto() {
        return new UpdateActivityRequestDto(
                "Titre modifié", "Desc", 1L,
                LocalDate.of(2026, 5, 10),
                LocalTime.of(14, 0), LocalTime.of(16, 0), 10,
                new LocationDto("1 rue A", null, "75001", "Paris"));
    }

    @Test
    void should_return_200_ok_on_update() {
        JwtPrincipal principal = new JwtPrincipal(3L, "u@entreprise.com", Role.COLLABORATOR);
        UpdateActivityRequestDto dto = updateDto();

        when(activityUseCase.update(eq(3L), eq(false), eq(10L), any(UpdateActivityRequestDto.class)))
                .thenReturn(sampleResponse());

        ResponseEntity<ActivityResponseDto> response = activityController.update(principal, 10L, dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(42L);
        assertThat(response.getBody().title()).isEqualTo("Titre modifié");
    }

    @Test
    void should_pass_is_admin_false_for_collaborator() {
        JwtPrincipal principal = new JwtPrincipal(3L, "u@entreprise.com", Role.COLLABORATOR);

        when(activityUseCase.update(eq(3L), eq(false), eq(10L), any(UpdateActivityRequestDto.class)))
                .thenReturn(sampleResponse());

        activityController.update(principal, 10L, updateDto());

        verify(activityUseCase).update(3L, false, 10L, updateDto());
    }

    @Test
    void should_pass_is_admin_true_for_admin() {
        JwtPrincipal principal = new JwtPrincipal(1L, "admin@entreprise.com", Role.ADMIN);

        when(activityUseCase.update(eq(1L), eq(true), eq(10L), any(UpdateActivityRequestDto.class)))
                .thenReturn(sampleResponse());

        activityController.update(principal, 10L, updateDto());

        verify(activityUseCase).update(1L, true, 10L, updateDto());
    }

    @Test
    void should_return_204_no_content_on_delete() {
        JwtPrincipal principal = new JwtPrincipal(3L, "u@entreprise.com", Role.COLLABORATOR);
        doNothing().when(activityUseCase).delete(3L, false, 10L);

        ResponseEntity<Void> response = activityController.delete(principal, 10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(activityUseCase).delete(3L, false, 10L);
    }

    @Test
    void should_pass_is_admin_true_on_delete_for_admin() {
        JwtPrincipal principal = new JwtPrincipal(1L, "admin@entreprise.com", Role.ADMIN);
        doNothing().when(activityUseCase).delete(1L, true, 99L);

        activityController.delete(principal, 99L);

        verify(activityUseCase).delete(1L, true, 99L);
    }
}
