package com.colife.api.material.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colife.api.activity.domain.Activity;
import com.colife.api.activity.domain.ActivityRepositoryPort;
import com.colife.api.material.application.dto.MaterialRequestDto;
import com.colife.api.material.application.dto.MaterialResponseDto;
import com.colife.api.material.domain.MaterialProposal;
import com.colife.api.material.domain.MaterialRepositoryPort;
import com.colife.api.shared.exception.BusinessException;
import com.colife.api.shared.exception.ForbiddenException;
import com.colife.api.shared.exception.ResourceNotFoundException;
import com.colife.api.subscription.domain.SubscriptionRepositoryPort;
import com.colife.api.user.domain.UserRepositoryPort;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class MaterialUseCase {

    private final MaterialRepositoryPort materialRepository;
    private final ActivityRepositoryPort activityRepository;
    private final SubscriptionRepositoryPort subscriptionRepository;
    private final UserRepositoryPort userRepository;
    private final Clock clock;

    public MaterialUseCase(
            MaterialRepositoryPort materialRepository,
            ActivityRepositoryPort activityRepository,
            SubscriptionRepositoryPort subscriptionRepository,
            UserRepositoryPort userRepository,
            Clock clock) {
        this.materialRepository = materialRepository;
        this.activityRepository = activityRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<MaterialResponseDto> listMaterials(Long activityId, UUID callerId) {
        findValidActivity(activityId);
        return materialRepository.findAllByActivityId(activityId).stream()
                .map(proposal -> toResponse(proposal, callerId))
                .toList();
    }

    @Transactional
    public MaterialResponseDto proposeMaterial(Long activityId, UUID userId, MaterialRequestDto dto) {
        Activity activity = findValidActivity(activityId);
        requireActivityNotPast(activity);

        boolean isOrganizer = activity.getOrganizerId().equals(userId);
        if (!isOrganizer && !subscriptionRepository.existsByActivityIdAndUserId(activityId, userId)) {
            throw new BusinessException("Vous devez être inscrit à l'activité pour proposer du matériel");
        }

        MaterialProposal proposal = MaterialProposal.builder()
                .activityId(activityId)
                .proposedBy(userId)
                .description(dto.description())
                .quantity(dto.quantity())
                .createdAt(LocalDateTime.now(clock))
                .deleted(false)
                .build();

        MaterialProposal saved = materialRepository.save(proposal);
        return toResponse(saved, userId);
    }

    @Transactional
    public MaterialResponseDto updateMaterial(Long activityId, Long materialId, UUID userId, MaterialRequestDto dto) {
        Activity activity = findValidActivity(activityId);
        requireActivityNotPast(activity);

        MaterialProposal existing = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposition de matériel non trouvée"));

        if (!existing.getActivityId().equals(activityId)) {
            throw new ResourceNotFoundException("Proposition de matériel non trouvée pour cette activité");
        }
        if (!existing.getProposedBy().equals(userId)) {
            throw new ForbiddenException("Vous ne pouvez modifier que vos propres propositions de matériel");
        }

        MaterialProposal updated = MaterialProposal.builder()
                .id(existing.getId())
                .activityId(existing.getActivityId())
                .proposedBy(existing.getProposedBy())
                .description(dto.description())
                .quantity(dto.quantity())
                .createdAt(existing.getCreatedAt())
                .deleted(existing.isDeleted())
                .build();

        MaterialProposal saved = materialRepository.save(updated);
        return toResponse(saved, userId);
    }

    @Transactional
    public void removeMaterial(Long activityId, Long materialId, UUID userId) {
        findValidActivity(activityId);

        MaterialProposal proposal = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposition de matériel non trouvée"));

        if (!proposal.getActivityId().equals(activityId)) {
            throw new ResourceNotFoundException("Proposition de matériel non trouvée pour cette activité");
        }
        if (!proposal.getProposedBy().equals(userId)) {
            throw new ForbiddenException("Vous ne pouvez retirer que vos propres propositions de matériel");
        }

        materialRepository.deleteById(materialId);
    }

    private Activity findValidActivity(Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activité non trouvée"));
        if (activity.isDeleted()) {
            throw new ResourceNotFoundException("Activité non trouvée");
        }
        return activity;
    }

    private void requireActivityNotPast(Activity activity) {
        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);
        if (activity.getDate().isBefore(today)
                || (activity.getDate().isEqual(today) && !now.isBefore(activity.getStartTime()))) {
            throw new BusinessException("L'activité est déjà passée ou en cours");
        }
    }

    private MaterialResponseDto toResponse(MaterialProposal proposal, UUID callerId) {
        String proposedByName = userRepository.findById(proposal.getProposedBy())
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse("Inconnu");
        return new MaterialResponseDto(
                proposal.getId(),
                proposal.getActivityId(),
                proposedByName,
                proposal.getProposedBy().equals(callerId),
                proposal.getDescription(),
                proposal.getQuantity(),
                proposal.getCreatedAt()
        );
    }
}
