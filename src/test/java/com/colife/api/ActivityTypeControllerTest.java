package com.colife.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.colife.api.activityType.application.ActivityTypeUseCase;
import com.colife.api.activityType.application.dto.ActivityTypeCountDto;
import com.colife.api.activityType.application.dto.ActivityTypeRequestDto;
import com.colife.api.activityType.application.dto.ActivityTypeResponseDto;
import com.colife.api.activityType.presentation.ActivityTypeController;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityTypeControllerTest {

    @Mock
    private ActivityTypeUseCase activityTypeUseCase;

    @InjectMocks
    private ActivityTypeController controller;

    @Test
    void findAll_should_return_200_with_list() {
        when(activityTypeUseCase.findAll()).thenReturn(List.of(
                new ActivityTypeResponseDto(1L, "Sport"),
                new ActivityTypeResponseDto(2L, "Culture")
        ));

        ResponseEntity<List<ActivityTypeResponseDto>> response = controller.findAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void count_should_return_200_with_count() {
        when(activityTypeUseCase.count()).thenReturn(new ActivityTypeCountDto(3L));

        ResponseEntity<ActivityTypeCountDto> response = controller.count();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().count()).isEqualTo(3L);
    }

    @Test
    void findById_should_return_200_with_type() {
        when(activityTypeUseCase.findById(1L)).thenReturn(new ActivityTypeResponseDto(1L, "Sport"));

        ResponseEntity<ActivityTypeResponseDto> response = controller.findById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Sport");
    }

    @Test
    void create_should_return_201_with_created_type() {
        ActivityTypeRequestDto dto = new ActivityTypeRequestDto("Musique");
        when(activityTypeUseCase.create(dto)).thenReturn(new ActivityTypeResponseDto(10L, "Musique"));

        ResponseEntity<ActivityTypeResponseDto> response = controller.create(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(10L);
        verify(activityTypeUseCase).create(dto);
    }

    @Test
    void update_should_return_200_with_updated_type() {
        ActivityTypeRequestDto dto = new ActivityTypeRequestDto("Musique");
        when(activityTypeUseCase.update(1L, dto)).thenReturn(new ActivityTypeResponseDto(1L, "Musique"));

        ResponseEntity<ActivityTypeResponseDto> response = controller.update(1L, dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Musique");
        verify(activityTypeUseCase).update(1L, dto);
    }

    @Test
    void delete_should_return_204_no_content() {
        doNothing().when(activityTypeUseCase).delete(5L);

        ResponseEntity<Void> response = controller.delete(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(activityTypeUseCase).delete(5L);
    }
}
