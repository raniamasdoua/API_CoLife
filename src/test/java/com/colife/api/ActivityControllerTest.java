package com.colife.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.colife.api.activity.application.ActivityUseCase;
import com.colife.api.activity.application.dto.ActivityResponseDto;
import com.colife.api.activity.application.dto.ActivityTypeDto;
import com.colife.api.activity.application.dto.CreateActivityRequestDto;
import com.colife.api.activity.application.dto.LocationDto;
import com.colife.api.activity.application.dto.ParticipantDto;
import com.colife.api.activity.application.dto.UpdateActivityRequestDto;
import com.colife.api.activity.domain.LocationType;
import com.colife.api.activity.presentation.ActivityController;
import com.colife.api.shared.security.JwtPrincipal;
import com.colife.api.user.domain.Role;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityControllerTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private ActivityUseCase activityUseCase;

    @InjectMocks
    private ActivityController activityController;

    @Test
    void should_return_201_created() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, "u@entreprise.com", Role.COLLABORATOR);
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
                "Atelier",
                null,
                1L,
                LocalDate.of(2026, 5, 1),
                LocalTime.of(15, 0),
                LocalTime.of(17, 0),
                12,
                new LocationDto(null, "5 av", "Bât B", "69000", "Lyon"),
                LocationType.OFF_SITE,
                null,
                null);

        ActivityResponseDto responseDto = new ActivityResponseDto(
                42L,
                "Atelier",
                null,
                12,
                1,
                new LocationDto(null, "5 av", "Bât B", "69000", "Lyon"),
                new ActivityTypeDto(1L, "Culture"),
                LocalDate.of(2026, 5, 1),
                LocalTime.of(15, 0),
                LocalTime.of(17, 0),
                "Organisateur Test",
                false,
                LocationType.OFF_SITE,
                null,
                List.of());

        when(activityUseCase.create(eq(USER_ID), any(CreateActivityRequestDto.class))).thenReturn(responseDto);

        ResponseEntity<ActivityResponseDto> response = activityController.create(principal, dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(42L);
        verify(activityUseCase).create(USER_ID, dto);
    }

    // ─── Tests : PUT /activities/{id} ───────────────────────────────────────

    private ActivityResponseDto sampleResponse() {
        return new ActivityResponseDto(
                42L, "Titre modifié", "Desc", 10, 1,
                new LocationDto(null, "1 rue A", null, "75001", "Paris"),
                new ActivityTypeDto(1L, "Sport"),
                LocalDate.of(2026, 5, 10),
                LocalTime.of(14, 0), LocalTime.of(16, 0),
                "Organisateur Test",
                false,
                LocationType.OFF_SITE,
                null,
                List.of());
    }

    private UpdateActivityRequestDto updateDto() {
        return new UpdateActivityRequestDto(
                "Titre modifié", "Desc", 1L,
                LocalDate.of(2026, 5, 10),
                LocalTime.of(14, 0), LocalTime.of(16, 0), 10,
                new LocationDto(null, "1 rue A", null, "75001", "Paris"),
                LocationType.OFF_SITE);
    }

    @Test
    void should_return_200_ok_on_update() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, "u@entreprise.com", Role.COLLABORATOR);
        UpdateActivityRequestDto dto = updateDto();

        when(activityUseCase.update(eq(USER_ID), eq(false), eq(10L), any(UpdateActivityRequestDto.class)))
                .thenReturn(sampleResponse());

        ResponseEntity<ActivityResponseDto> response = activityController.update(principal, 10L, dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(42L);
        assertThat(response.getBody().title()).isEqualTo("Titre modifié");
    }

    @Test
    void should_pass_is_admin_false_for_collaborator() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, "u@entreprise.com", Role.COLLABORATOR);

        when(activityUseCase.update(eq(USER_ID), eq(false), eq(10L), any(UpdateActivityRequestDto.class)))
                .thenReturn(sampleResponse());

        activityController.update(principal, 10L, updateDto());

        verify(activityUseCase).update(USER_ID, false, 10L, updateDto());
    }

    @Test
    void should_pass_is_admin_true_for_admin() {
        JwtPrincipal principal = new JwtPrincipal(ADMIN_ID, "admin@entreprise.com", Role.ADMIN);

        when(activityUseCase.update(eq(ADMIN_ID), eq(true), eq(10L), any(UpdateActivityRequestDto.class)))
                .thenReturn(sampleResponse());

        activityController.update(principal, 10L, updateDto());

        verify(activityUseCase).update(ADMIN_ID, true, 10L, updateDto());
    }

    @Test
    void should_return_204_no_content_on_delete() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, "u@entreprise.com", Role.COLLABORATOR);
        doNothing().when(activityUseCase).delete(USER_ID, false, 10L);

        ResponseEntity<Void> response = activityController.delete(principal, 10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(activityUseCase).delete(USER_ID, false, 10L);
    }

    @Test
    void should_pass_is_admin_true_on_delete_for_admin() {
        JwtPrincipal principal = new JwtPrincipal(ADMIN_ID, "admin@entreprise.com", Role.ADMIN);
        doNothing().when(activityUseCase).delete(ADMIN_ID, true, 99L);

        activityController.delete(principal, 99L);

        verify(activityUseCase).delete(ADMIN_ID, true, 99L);
    }

    // ─── Tests : GET /activities ─────────────────────────────────────────────

    @Test
    void should_return_200_on_get_all_activities() {
        when(activityUseCase.getAllActivities(false)).thenReturn(List.of(sampleResponse()));

        ResponseEntity<List<ActivityResponseDto>> response = activityController.getAllActivities(false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(activityUseCase).getAllActivities(false);
    }

    @Test
    void should_return_200_on_get_participants() {
        ParticipantDto participant = new ParticipantDto(USER_ID, "Jean", "Dupont", "jean@test.com");
        when(activityUseCase.getParticipants(10L)).thenReturn(List.of(participant));

        ResponseEntity<List<ParticipantDto>> response = activityController.getParticipants(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(activityUseCase).getParticipants(10L);
    }

    // ─── Tests : GET /activities/mine|registered|available ───────────────────

    @Test
    void should_return_200_on_get_my_activities() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, "u@entreprise.com", Role.COLLABORATOR);
        when(activityUseCase.getMyActivities(USER_ID)).thenReturn(List.of(sampleResponse()));

        ResponseEntity<List<ActivityResponseDto>> response = activityController.getMyActivities(principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(activityUseCase).getMyActivities(USER_ID);
    }

    @Test
    void should_return_200_on_get_registered_activities() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, "u@entreprise.com", Role.COLLABORATOR);
        when(activityUseCase.getRegisteredActivities(USER_ID)).thenReturn(List.of(sampleResponse()));

        ResponseEntity<List<ActivityResponseDto>> response = activityController.getRegisteredActivities(principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(activityUseCase).getRegisteredActivities(USER_ID);
    }

    @Test
    void should_return_200_on_get_available_activities() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, "u@entreprise.com", Role.COLLABORATOR);
        when(activityUseCase.getAvailableActivities(USER_ID)).thenReturn(List.of(sampleResponse()));

        ResponseEntity<List<ActivityResponseDto>> response = activityController.getAvailableActivities(principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(activityUseCase).getAvailableActivities(USER_ID);
    }

    // ─── Tests : POST/DELETE /activities/{id}/subscribe ──────────────────────

    @Test
    void should_return_201_on_subscribe() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, "u@entreprise.com", Role.COLLABORATOR);
        when(activityUseCase.subscribe(USER_ID, 10L)).thenReturn(sampleResponse());

        ResponseEntity<ActivityResponseDto> response = activityController.subscribe(principal, 10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        verify(activityUseCase).subscribe(USER_ID, 10L);
    }

    @Test
    void should_return_200_on_unsubscribe() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, "u@entreprise.com", Role.COLLABORATOR);
        when(activityUseCase.unsubscribe(USER_ID, 10L)).thenReturn(sampleResponse());

        ResponseEntity<ActivityResponseDto> response = activityController.unsubscribe(principal, 10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        verify(activityUseCase).unsubscribe(USER_ID, 10L);
    }
}
