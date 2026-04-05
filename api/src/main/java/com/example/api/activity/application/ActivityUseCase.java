package com.example.api.activity.application;

import com.example.api.activity.application.dto.ActivityResponseDto;
import com.example.api.activity.application.dto.ActivityTypeDto;
import com.example.api.activity.application.dto.CreateActivityRequestDto;
import com.example.api.activity.application.dto.LocationDto;
import com.example.api.activity.application.dto.UpdateActivityRequestDto;
import com.example.api.activity.domain.Activity;
import com.example.api.activity.domain.ActivityCreationPolicy;
import com.example.api.activity.domain.ActivityRepositoryPort;
import com.example.api.activity.domain.ActivityUpdatePolicy;
import com.example.api.activity.domain.Location;
import com.example.api.activityType.domain.ActivityType;
import com.example.api.activityType.domain.ActivityTypeRepositoryPort;
import com.example.api.shared.exception.ConflictException;
import com.example.api.shared.exception.ForbiddenException;
import com.example.api.shared.exception.ResourceNotFoundException;
import com.example.api.subscription.domain.SubscriptionRepositoryPort;
import com.example.api.user.domain.UserRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ActivityUseCase {

    private final ActivityRepositoryPort activityRepository;
    private final ActivityTypeRepositoryPort activityTypeRepository;
    private final UserRepositoryPort userRepository;
    private final SubscriptionRepositoryPort subscriptionRepository;
    private final Clock clock;

    public ActivityUseCase(
            ActivityRepositoryPort activityRepository,
            ActivityTypeRepositoryPort activityTypeRepository,
            UserRepositoryPort userRepository,
            SubscriptionRepositoryPort subscriptionRepository,
            Clock clock) {
        this.activityRepository = activityRepository;
        this.activityTypeRepository = activityTypeRepository;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.clock = clock;
    }

    @Transactional
    public ActivityResponseDto create(Long organizerId, CreateActivityRequestDto dto) {
        userRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        ActivityType activityType = activityTypeRepository.findById(dto.activityTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Type d'activité non trouvé"));

        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);
        ActivityCreationPolicy.validate(dto.date(), today, now, dto.capacity(), dto.startTime(), dto.endTime());

        if (activityRepository.existsOverlappingForOrganizer(organizerId, dto.date(), dto.startTime(), dto.endTime())) {
            throw new ConflictException("Vous avez déjà une activité sur ce créneau horaire");
        }

        Location location = Location.builder()
                .street(dto.location().street())
                .complement(dto.location().complement())
                .postalCode(dto.location().postalCode())
                .city(dto.location().city())
                .build();

        Activity activity = Activity.builder()
                .title(dto.title())
                .description(dto.description())
                .capacity(dto.capacity())
                .location(location)
                .typeId(activityType.getId())
                .organizerId(organizerId)
                .date(dto.date())
                .startTime(dto.startTime())
                .endTime(dto.endTime())
                .deleted(false)
                .build();

        Activity saved = activityRepository.save(activity);

        subscriptionRepository.registerParticipant(saved.getId(), organizerId);

        String organizerName = userRepository.findById(organizerId)
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse("Inconnu");
        int participantCount = subscriptionRepository.countParticipants(saved.getId());
        return toResponse(saved, activityType, organizerName, participantCount);
    }

    @Transactional
    public ActivityResponseDto update(Long callerId, boolean isAdmin, Long activityId, UpdateActivityRequestDto dto) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activité non trouvée"));
        if (activity.isDeleted()) {
            throw new ResourceNotFoundException("Activité non trouvée");
        }

        if (!isAdmin && !activity.getOrganizerId().equals(callerId)) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à modifier cette activité");
        }

        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);
        ActivityUpdatePolicy.validateActivityIsModifiable(activity, today, now);

        ActivityType activityType = activityTypeRepository.findById(dto.activityTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Type d'activité non trouvé"));

        int participantCount = subscriptionRepository.countParticipants(activityId);
        ActivityUpdatePolicy.validateNewSlot(dto.date(), today, now, dto.startTime(), dto.endTime(), dto.capacity(), participantCount);

        List<Long> participantIds = subscriptionRepository.findUserIdsByActivityId(activityId);

        List<Long> participantOnlyIds = participantIds.stream()
                .filter(id -> !id.equals(activity.getOrganizerId()))
                .toList();

        boolean organizerConflictAsOrganizer = activityRepository.existsOverlappingForUsersAsOrganizer(
                List.of(activity.getOrganizerId()), dto.date(), dto.startTime(), dto.endTime(), activityId);
        boolean organizerConflictAsParticipant = subscriptionRepository.existsConflictingActivityForSubscribedUsers(
                List.of(activity.getOrganizerId()), dto.date(), dto.startTime(), dto.endTime(), activityId);

        if (organizerConflictAsOrganizer || organizerConflictAsParticipant) {
            throw new ConflictException("Le nouvel horaire entre en conflit avec une autre activité de l'organisateur");
        }

        if (!participantOnlyIds.isEmpty()) {
            boolean participantConflictAsOrganizer = activityRepository.existsOverlappingForUsersAsOrganizer(
                    participantOnlyIds, dto.date(), dto.startTime(), dto.endTime(), activityId);
            boolean participantConflictAsParticipant = subscriptionRepository.existsConflictingActivityForSubscribedUsers(
                    participantOnlyIds, dto.date(), dto.startTime(), dto.endTime(), activityId);

            if (participantConflictAsOrganizer || participantConflictAsParticipant) {
                throw new ConflictException("Le nouvel horaire entre en conflit avec le planning d'un ou plusieurs participants");
            }
        }

        Location location = Location.builder()
                .street(dto.location().street())
                .complement(dto.location().complement())
                .postalCode(dto.location().postalCode())
                .city(dto.location().city())
                .build();

        Activity updated = Activity.builder()
                .id(activity.getId())
                .title(dto.title())
                .description(dto.description())
                .capacity(dto.capacity())
                .location(location)
                .typeId(activityType.getId())
                .organizerId(activity.getOrganizerId())
                .date(dto.date())
                .startTime(dto.startTime())
                .endTime(dto.endTime())
                .deleted(activity.isDeleted())
                .build();

        Activity saved = activityRepository.update(updated);

        String organizerName = userRepository.findById(activity.getOrganizerId())
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse("Inconnu");
        int updatedParticipantCount = subscriptionRepository.countParticipants(saved.getId());
        return toResponse(saved, activityType, organizerName, updatedParticipantCount);
    }

    @Transactional
    public void delete(Long callerId, boolean isAdmin, Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activité non trouvée"));
        if (activity.isDeleted()) {
            throw new ResourceNotFoundException("Activité non trouvée");
        }

        if (!isAdmin && !activity.getOrganizerId().equals(callerId)) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à supprimer cette activité");
        }

        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);
        ActivityUpdatePolicy.validateActivityIsModifiable(activity, today, now);

        subscriptionRepository.deleteAllByActivityId(activityId);
        activityRepository.softDelete(activityId);
    }

    @Transactional(readOnly = true)
    public List<ActivityResponseDto> getMyActivities(Long userId) {
        List<Activity> activities = activityRepository.findByOrganizerId(userId);
        return toResponseList(activities);
    }

    @Transactional(readOnly = true)
    public List<ActivityResponseDto> getAvailableActivities(Long userId) {
        List<Activity> activities = activityRepository.findByOrganizerIdNot(userId);
        return toResponseList(activities);
    }

    private List<ActivityResponseDto> toResponseList(List<Activity> activities) {
        Map<Long, ActivityType> typeCache = new HashMap<>();
        Map<Long, String> organizerCache = new HashMap<>();
        return activities.stream()
                .map(activity -> {
                    ActivityType type = typeCache.computeIfAbsent(
                            activity.getTypeId(),
                            id -> activityTypeRepository.findById(id)
                                    .orElseThrow(() -> new ResourceNotFoundException("Type d'activité non trouvé"))
                    );
                    String organizerName = organizerCache.computeIfAbsent(
                            activity.getOrganizerId(),
                            id -> userRepository.findById(id)
                                    .map(u -> u.getFirstName() + " " + u.getLastName())
                                    .orElse("Inconnu")
                    );
                    int participantCount = subscriptionRepository.countParticipants(activity.getId());
                    return toResponse(activity, type, organizerName, participantCount);
                })
                .toList();
    }

    private ActivityResponseDto toResponse(Activity activity, ActivityType type, String organizerName, int participantCount) {
        LocationDto locationDto = new LocationDto(
                activity.getLocation().getStreet(),
                activity.getLocation().getComplement(),
                activity.getLocation().getPostalCode(),
                activity.getLocation().getCity()
        );
        ActivityTypeDto typeDto = new ActivityTypeDto(type.getId(), type.getName());
        return new ActivityResponseDto(
                activity.getId(),
                activity.getTitle(),
                activity.getDescription(),
                activity.getCapacity(),
                participantCount,
                locationDto,
                typeDto,
                activity.getDate(),
                activity.getStartTime(),
                activity.getEndTime(),
                organizerName
        );
    }
}
