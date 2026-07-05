package com.colife.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.colife.api.activityType.application.ActivityTypeUseCase;
import com.colife.api.activityType.application.dto.ActivityTypeCountDto;
import com.colife.api.activityType.application.dto.ActivityTypeRequestDto;
import com.colife.api.activityType.application.dto.ActivityTypeResponseDto;
import com.colife.api.activityType.domain.ActivityType;
import com.colife.api.activityType.domain.ActivityTypeRepositoryPort;
import com.colife.api.shared.exception.ConflictException;
import com.colife.api.shared.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityTypeUseCaseTest {

    @Mock
    private ActivityTypeRepositoryPort repository;

    @InjectMocks
    private ActivityTypeUseCase useCase;

    // ─── findAll ────────────────────────────────────────────────────────────

    @Test
    void findAll_should_return_list_of_active_types() {
        when(repository.findAllActive()).thenReturn(List.of(
                ActivityType.builder().id(1L).name("Sport").build(),
                ActivityType.builder().id(2L).name("Culture").build()
        ));

        List<ActivityTypeResponseDto> result = useCase.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Sport");
        assertThat(result.get(1).name()).isEqualTo("Culture");
    }

    @Test
    void findAll_should_return_empty_list_when_no_types() {
        when(repository.findAllActive()).thenReturn(List.of());

        assertThat(useCase.findAll()).isEmpty();
    }

    // ─── count ──────────────────────────────────────────────────────────────

    @Test
    void count_should_return_active_count() {
        when(repository.countActive()).thenReturn(5L);

        ActivityTypeCountDto result = useCase.count();

        assertThat(result.count()).isEqualTo(5L);
    }

    // ─── findById ───────────────────────────────────────────────────────────

    @Test
    void findById_should_return_type_when_found() {
        when(repository.findActiveById(1L)).thenReturn(
                Optional.of(ActivityType.builder().id(1L).name("Sport").build())
        );

        ActivityTypeResponseDto result = useCase.findById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Sport");
    }

    @Test
    void findById_should_throw_when_not_found() {
        when(repository.findActiveById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── create ─────────────────────────────────────────────────────────────

    @Test
    void create_should_save_and_return_new_type() {
        when(repository.existsActiveByNameIgnoreCase("Sport")).thenReturn(false);
        when(repository.save(any())).thenReturn(
                ActivityType.builder().id(10L).name("Sport").build()
        );

        ActivityTypeResponseDto result = useCase.create(new ActivityTypeRequestDto("Sport"));

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.name()).isEqualTo("Sport");
    }

    @Test
    void create_should_throw_conflict_when_name_already_exists() {
        when(repository.existsActiveByNameIgnoreCase("Sport")).thenReturn(true);

        assertThatThrownBy(() -> useCase.create(new ActivityTypeRequestDto("Sport")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_should_trim_name_before_saving() {
        when(repository.existsActiveByNameIgnoreCase("Sport")).thenReturn(false);
        when(repository.save(any())).thenReturn(
                ActivityType.builder().id(11L).name("Sport").build()
        );

        ActivityTypeResponseDto result = useCase.create(new ActivityTypeRequestDto("  Sport  "));

        assertThat(result.name()).isEqualTo("Sport");
    }

    // ─── update ─────────────────────────────────────────────────────────────

    @Test
    void update_should_save_and_return_updated_type() {
        when(repository.findActiveById(1L)).thenReturn(
                Optional.of(ActivityType.builder().id(1L).name("Sport").build())
        );
        when(repository.existsActiveByNameIgnoreCase("Musique")).thenReturn(false);
        when(repository.save(any())).thenReturn(
                ActivityType.builder().id(1L).name("Musique").build()
        );

        ActivityTypeResponseDto result = useCase.update(1L, new ActivityTypeRequestDto("Musique"));

        assertThat(result.name()).isEqualTo("Musique");
    }

    @Test
    void update_should_allow_same_name_case_insensitive() {
        when(repository.findActiveById(1L)).thenReturn(
                Optional.of(ActivityType.builder().id(1L).name("Sport").build())
        );
        when(repository.save(any())).thenReturn(
                ActivityType.builder().id(1L).name("SPORT").build()
        );

        ActivityTypeResponseDto result = useCase.update(1L, new ActivityTypeRequestDto("SPORT"));

        assertThat(result.name()).isEqualTo("SPORT");
    }

    @Test
    void update_should_throw_when_not_found() {
        when(repository.findActiveById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.update(99L, new ActivityTypeRequestDto("Sport")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_should_throw_conflict_when_new_name_already_exists() {
        when(repository.findActiveById(1L)).thenReturn(
                Optional.of(ActivityType.builder().id(1L).name("Sport").build())
        );
        when(repository.existsActiveByNameIgnoreCase("Musique")).thenReturn(true);

        assertThatThrownBy(() -> useCase.update(1L, new ActivityTypeRequestDto("Musique")))
                .isInstanceOf(ConflictException.class);
    }

    // ─── delete ─────────────────────────────────────────────────────────────

    @Test
    void delete_should_call_soft_delete() {
        useCase.delete(5L);

        verify(repository).softDeleteById(5L);
    }
}
