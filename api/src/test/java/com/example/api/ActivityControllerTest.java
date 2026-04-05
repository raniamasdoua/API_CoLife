package com.example.api;

import com.example.api.activity.application.ActivityUseCase;
import com.example.api.activity.application.dto.ActivityResponseDto;
import com.example.api.activity.application.dto.ActivityTypeDto;
import com.example.api.activity.application.dto.CreateActivityRequestDto;
import com.example.api.activity.application.dto.LocationDto;
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
}
