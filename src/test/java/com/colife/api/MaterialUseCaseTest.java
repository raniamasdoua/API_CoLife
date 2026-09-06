package com.colife.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.colife.api.activity.domain.Activity;
import com.colife.api.activity.domain.ActivityRepositoryPort;
import com.colife.api.activity.domain.Location;
import com.colife.api.activity.domain.LocationType;
import com.colife.api.material.application.MaterialUseCase;
import com.colife.api.material.application.dto.MaterialRequestDto;
import com.colife.api.material.application.dto.MaterialResponseDto;
import com.colife.api.material.domain.MaterialProposal;
import com.colife.api.material.domain.MaterialRepositoryPort;
import com.colife.api.shared.exception.BusinessException;
import com.colife.api.shared.exception.ForbiddenException;
import com.colife.api.shared.exception.ResourceNotFoundException;
import com.colife.api.subscription.domain.SubscriptionRepositoryPort;
import com.colife.api.user.domain.User;
import com.colife.api.user.domain.UserRepositoryPort;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialUseCaseTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-03-22T12:00:00Z"),
            ZoneId.of("Europe/Paris"));

    private static final UUID ORGANIZER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PARTICIPANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final Long ACTIVITY_ID = 100L;
    private static final Long MATERIAL_ID = 300L;

    @Mock
    private MaterialRepositoryPort materialRepository;
    @Mock
    private ActivityRepositoryPort activityRepository;
    @Mock
    private SubscriptionRepositoryPort subscriptionRepository;
    @Mock
    private UserRepositoryPort userRepository;

    private MaterialUseCase materialUseCase;

    @BeforeEach
    void setUp() {
        materialUseCase = new MaterialUseCase(
                materialRepository,
                activityRepository,
                subscriptionRepository,
                userRepository,
                FIXED_CLOCK);
    }

    private Activity futureActivity() {
        return Activity.builder()
                .id(ACTIVITY_ID).title("Sortie").capacity(10)
                .location(Location.builder().locationType(LocationType.OFF_SITE)
                        .street("r").postalCode("p").city("c").build())
                .typeId(2L).organizerId(ORGANIZER_ID)
                .date(LocalDate.of(2026, Month.MARCH, 30))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(12, 0))
                .deleted(false).locationType(LocationType.OFF_SITE).build();
    }

    private Activity pastActivity() {
        return Activity.builder()
                .id(ACTIVITY_ID).title("Sortie").capacity(10)
                .location(Location.builder().locationType(LocationType.OFF_SITE)
                        .street("r").postalCode("p").city("c").build())
                .typeId(2L).organizerId(ORGANIZER_ID)
                .date(LocalDate.of(2026, Month.MARCH, 20))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(12, 0))
                .deleted(false).locationType(LocationType.OFF_SITE).build();
    }

    private MaterialProposal proposal(UUID proposedBy) {
        return MaterialProposal.builder()
                .id(MATERIAL_ID).activityId(ACTIVITY_ID).proposedBy(proposedBy)
                .description("Ballon de foot").quantity(1)
                .createdAt(LocalDateTime.of(2026, Month.MARCH, 15, 10, 0))
                .build();
    }

    // ─── listMaterials ──────────────────────────────────────────────────────

    @Test
    void listMaterials_should_return_proposals_for_activity() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(materialRepository.findAllByActivityId(ACTIVITY_ID)).thenReturn(List.of(proposal(PARTICIPANT_ID)));
        when(userRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(mock(User.class)));

        List<MaterialResponseDto> result = materialUseCase.listMaterials(ACTIVITY_ID, PARTICIPANT_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).description()).isEqualTo("Ballon de foot");
        assertThat(result.get(0).mine()).isTrue();
    }

    @Test
    void listMaterials_should_mark_proposal_as_not_mine_for_another_user() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(materialRepository.findAllByActivityId(ACTIVITY_ID)).thenReturn(List.of(proposal(PARTICIPANT_ID)));
        when(userRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(mock(User.class)));

        List<MaterialResponseDto> result = materialUseCase.listMaterials(ACTIVITY_ID, OTHER_USER_ID);

        assertThat(result.get(0).mine()).isFalse();
    }

    @Test
    void listMaterials_should_throw_when_activity_not_found() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> materialUseCase.listMaterials(ACTIVITY_ID, PARTICIPANT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── proposeMaterial ────────────────────────────────────────────────────

    @Test
    void proposeMaterial_should_succeed_for_subscribed_participant() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(subscriptionRepository.existsByActivityIdAndUserId(ACTIVITY_ID, PARTICIPANT_ID)).thenReturn(true);
        when(materialRepository.save(any(MaterialProposal.class))).thenReturn(proposal(PARTICIPANT_ID));
        when(userRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(mock(User.class)));

        MaterialResponseDto result = materialUseCase.proposeMaterial(
                ACTIVITY_ID, PARTICIPANT_ID, new MaterialRequestDto("Ballon de foot", 1));

        assertThat(result.description()).isEqualTo("Ballon de foot");
        verify(materialRepository).save(any(MaterialProposal.class));
    }

    @Test
    void proposeMaterial_should_succeed_for_organizer_without_subscription() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(materialRepository.save(any(MaterialProposal.class))).thenReturn(proposal(ORGANIZER_ID));
        when(userRepository.findById(ORGANIZER_ID)).thenReturn(Optional.of(mock(User.class)));

        MaterialResponseDto result = materialUseCase.proposeMaterial(
                ACTIVITY_ID, ORGANIZER_ID, new MaterialRequestDto("Ballon de foot", 1));

        assertThat(result).isNotNull();
        verify(subscriptionRepository, never()).existsByActivityIdAndUserId(any(), any());
    }

    @Test
    void proposeMaterial_should_throw_when_not_subscribed_nor_organizer() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(subscriptionRepository.existsByActivityIdAndUserId(ACTIVITY_ID, OTHER_USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> materialUseCase.proposeMaterial(
                ACTIVITY_ID, OTHER_USER_ID, new MaterialRequestDto("Ballon de foot", 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inscrit");
    }

    @Test
    void proposeMaterial_should_throw_when_activity_is_past() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(pastActivity()));

        assertThatThrownBy(() -> materialUseCase.proposeMaterial(
                ACTIVITY_ID, ORGANIZER_ID, new MaterialRequestDto("Ballon de foot", 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("passée");
    }

    // ─── updateMaterial ─────────────────────────────────────────────────────

    @Test
    void updateMaterial_should_succeed_for_proposer() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(proposal(PARTICIPANT_ID)));
        when(materialRepository.save(any(MaterialProposal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(mock(User.class)));

        MaterialResponseDto result = materialUseCase.updateMaterial(
                ACTIVITY_ID, MATERIAL_ID, PARTICIPANT_ID, new MaterialRequestDto("Ballon de basket", 2));

        assertThat(result.description()).isEqualTo("Ballon de basket");
        assertThat(result.quantity()).isEqualTo(2);
        assertThat(result.mine()).isTrue();
    }

    @Test
    void updateMaterial_should_throw_when_not_the_proposer() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(proposal(PARTICIPANT_ID)));

        assertThatThrownBy(() -> materialUseCase.updateMaterial(
                ACTIVITY_ID, MATERIAL_ID, OTHER_USER_ID, new MaterialRequestDto("Ballon de basket", 2)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateMaterial_should_throw_when_material_not_found() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> materialUseCase.updateMaterial(
                ACTIVITY_ID, MATERIAL_ID, PARTICIPANT_ID, new MaterialRequestDto("Ballon de basket", 2)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateMaterial_should_throw_when_material_belongs_to_another_activity() {
        Long otherActivityId = 999L;
        when(activityRepository.findById(otherActivityId)).thenReturn(Optional.of(futureActivity()));
        when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(proposal(PARTICIPANT_ID)));

        assertThatThrownBy(() -> materialUseCase.updateMaterial(
                otherActivityId, MATERIAL_ID, PARTICIPANT_ID, new MaterialRequestDto("Ballon de basket", 2)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateMaterial_should_throw_when_activity_is_past() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(pastActivity()));

        assertThatThrownBy(() -> materialUseCase.updateMaterial(
                ACTIVITY_ID, MATERIAL_ID, PARTICIPANT_ID, new MaterialRequestDto("Ballon de basket", 2)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("passée");
    }

    // ─── removeMaterial ─────────────────────────────────────────────────────

    @Test
    void removeMaterial_should_succeed_for_proposer() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(proposal(PARTICIPANT_ID)));

        materialUseCase.removeMaterial(ACTIVITY_ID, MATERIAL_ID, PARTICIPANT_ID);

        verify(materialRepository).deleteById(MATERIAL_ID);
    }

    @Test
    void removeMaterial_should_throw_when_not_the_proposer() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(proposal(PARTICIPANT_ID)));

        assertThatThrownBy(() -> materialUseCase.removeMaterial(ACTIVITY_ID, MATERIAL_ID, OTHER_USER_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void removeMaterial_should_throw_when_material_not_found() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> materialUseCase.removeMaterial(ACTIVITY_ID, MATERIAL_ID, PARTICIPANT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeMaterial_should_throw_when_material_belongs_to_another_activity() {
        Long otherActivityId = 999L;
        when(activityRepository.findById(otherActivityId)).thenReturn(Optional.of(futureActivity()));
        when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(proposal(PARTICIPANT_ID)));

        assertThatThrownBy(() -> materialUseCase.removeMaterial(otherActivityId, MATERIAL_ID, PARTICIPANT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
