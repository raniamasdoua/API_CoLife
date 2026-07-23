package com.colife.api.activity.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colife.api.activity.application.dto.ActivityResponseDto;
import com.colife.api.activity.application.dto.ActivityTypeDto;
import com.colife.api.activity.application.dto.CreateActivityRequestDto;
import com.colife.api.activity.application.dto.LocationDto;
import com.colife.api.activity.application.dto.ParticipantDto;
import com.colife.api.activity.application.dto.UpdateActivityRequestDto;
import com.colife.api.activity.domain.Activity;
import com.colife.api.activity.domain.ActivityCreationPolicy;
import com.colife.api.activity.domain.ActivityRepositoryPort;
import com.colife.api.activity.domain.ActivitySubscriptionPolicy;
import com.colife.api.activity.domain.ActivityUpdatePolicy;
import com.colife.api.activity.domain.Location;
import com.colife.api.activity.domain.LocationType;
import com.colife.api.activityType.domain.ActivityType;
import com.colife.api.activityType.domain.ActivityTypeRepositoryPort;
import com.colife.api.carpool.application.dto.CarpoolResponseDto;
import com.colife.api.carpool.domain.Carpool;
import com.colife.api.carpool.domain.CarpoolCreationPolicy;
import com.colife.api.carpool.domain.CarpoolPassengerRepositoryPort;
import com.colife.api.carpool.domain.CarpoolRepositoryPort;
import com.colife.api.shared.exception.BusinessException;
import com.colife.api.shared.exception.ConflictException;
import com.colife.api.shared.exception.ForbiddenException;
import com.colife.api.shared.exception.ResourceNotFoundException;
import com.colife.api.subscription.domain.SubscriptionRepositoryPort;
import com.colife.api.user.domain.UserRepositoryPort;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class ActivityUseCase {

    private final ActivityRepositoryPort activityRepository;
    private final ActivityTypeRepositoryPort activityTypeRepository;
    private final UserRepositoryPort userRepository;
    private final SubscriptionRepositoryPort subscriptionRepository;
    private final CarpoolRepositoryPort carpoolRepository;
    private final CarpoolPassengerRepositoryPort carpoolPassengerRepository;
    private final Clock clock;

    public ActivityUseCase(
            ActivityRepositoryPort activityRepository,
            ActivityTypeRepositoryPort activityTypeRepository,
            UserRepositoryPort userRepository,
            SubscriptionRepositoryPort subscriptionRepository,
            CarpoolRepositoryPort carpoolRepository,
            CarpoolPassengerRepositoryPort carpoolPassengerRepository,
            Clock clock) {
        this.activityRepository = activityRepository;
        this.activityTypeRepository = activityTypeRepository;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.carpoolRepository = carpoolRepository;
        this.carpoolPassengerRepository = carpoolPassengerRepository;
        this.clock = clock;
    }

    @Transactional
    public ActivityResponseDto create(UUID organizerId, CreateActivityRequestDto dto) {
        userRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        ActivityType activityType = activityTypeRepository.findActiveById(dto.activityTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Type d'activité non trouvé"));

        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);
        ActivityCreationPolicy.validate(dto.date(), today, now, dto.capacity(), dto.startTime(), dto.endTime());

        if (activityRepository.existsOverlappingForOrganizer(organizerId, dto.date(), dto.startTime(), dto.endTime())) {
            throw new ConflictException("Vous avez déjà une activité sur ce créneau horaire");
        }

        if (subscriptionRepository.existsConflictingActivityForSubscribedUsers(
                List.of(organizerId), dto.date(), dto.startTime(), dto.endTime(), -1L)) {
            throw new ConflictException("Vous êtes déjà inscrit à une activité sur ce créneau horaire");
        }

        LocationType locationType = dto.locationType() != null ? dto.locationType() : LocationType.OFF_SITE;
        validateLocation(locationType, dto.location());

        if (dto.carpool() != null) {
            if (locationType == LocationType.ON_SITE) {
                throw new BusinessException("Le covoiturage n'est disponible que pour les activités hors site");
            }
            CarpoolCreationPolicy.validate(dto.carpool().maxPassengers(), dto.carpool().departureTime(), dto.startTime());
        }

        Location location = buildLocation(locationType, dto.location());

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
                .locationType(locationType)
                .build();

        Activity saved = activityRepository.save(activity);

        subscriptionRepository.registerParticipant(saved.getId(), organizerId);

        CarpoolResponseDto carpoolResponse = null;
        if (dto.carpool() != null) {
            Carpool carpool = Carpool.builder()
                    .activityId(saved.getId())
                    .driverId(organizerId)
                    .departureTime(dto.carpool().departureTime())
                    .maxPassengers(dto.carpool().maxPassengers())
                    .build();
            Carpool savedCarpool = carpoolRepository.save(carpool);
            carpoolResponse = toCarpoolResponse(savedCarpool);
        }

        String organizerName = userRepository.findById(organizerId)
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse("Inconnu");
        int participantCount = subscriptionRepository.countParticipants(saved.getId());
        return toResponse(saved, activityType, organizerName, participantCount, carpoolResponse);
    }

    private void validateLocation(LocationType locationType, LocationDto location) {
        if (locationType == LocationType.ON_SITE) {
            if (location.room() == null || location.room().isBlank()) {
                throw new BusinessException("La salle est obligatoire pour une activité sur site");
            }
        } else {
            if (location.street() == null || location.street().isBlank()) {
                throw new BusinessException("L'adresse (rue) est obligatoire pour une activité hors site");
            }
            if (location.postalCode() == null || location.postalCode().isBlank()) {
                throw new BusinessException("Le code postal est obligatoire pour une activité hors site");
            }
            if (location.city() == null || location.city().isBlank()) {
                throw new BusinessException("La ville est obligatoire pour une activité hors site");
            }
        }
    }

    private Location buildLocation(LocationType locationType, LocationDto dto) {
        return Location.builder()
                .locationType(locationType)
                .room(dto.room())
                .street(dto.street())
                .complement(dto.complement())
                .postalCode(dto.postalCode())
                .city(dto.city())
                .build();
    }

    private CarpoolResponseDto toCarpoolResponse(Carpool carpool) {
        return new CarpoolResponseDto(
                carpool.getId(),
                carpool.getActivityId(),
                carpool.getDriverId(),
                carpool.getDepartureTime(),
                carpool.getMaxPassengers()
        );
    }

    @Transactional
    public ActivityResponseDto update(UUID callerId, boolean isAdmin, Long activityId, UpdateActivityRequestDto dto) {
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

        ActivityType activityType = activityTypeRepository.findActiveById(dto.activityTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Type d'activité non trouvé"));

        int participantCount = subscriptionRepository.countParticipants(activityId);
        ActivityUpdatePolicy.validateNewSlot(dto.date(), today, now, dto.startTime(), dto.endTime(), dto.capacity(), participantCount);

        List<UUID> participantIds = subscriptionRepository.findUserIdsByActivityId(activityId);

        List<UUID> participantOnlyIds = participantIds.stream()
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

        LocationType locationType = dto.locationType() != null ? dto.locationType()
                : (activity.getLocationType() != null ? activity.getLocationType() : LocationType.OFF_SITE);
        validateLocation(locationType, dto.location());

        // Si l'activité passe de hors-site à sur-site, annuler tous les covoiturages actifs
        boolean switchingToOnSite = locationType == LocationType.ON_SITE
                && activity.getLocationType() != LocationType.ON_SITE;
        if (switchingToOnSite) {
            List<Carpool> activeCarpools = carpoolRepository.findAllActiveByActivityId(activityId);
            if (!activeCarpools.isEmpty()) {
                List<Long> carpoolIds = activeCarpools.stream().map(Carpool::getId).toList();
                carpoolPassengerRepository.removeAllByCarpoolIds(carpoolIds);
                carpoolRepository.cancelAllByActivityId(activityId);
            }
        }

        Location location = buildLocation(locationType, dto.location());

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
                .locationType(locationType)
                .build();

        Activity saved = activityRepository.update(updated);

        String organizerName = userRepository.findById(activity.getOrganizerId())
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse("Inconnu");
        int updatedParticipantCount = subscriptionRepository.countParticipants(saved.getId());
        CarpoolResponseDto carpoolResponse = carpoolRepository.findByActivityId(saved.getId())
                .map(this::toCarpoolResponse)
                .orElse(null);
        return toResponse(saved, activityType, organizerName, updatedParticipantCount, carpoolResponse);
    }

    @Transactional
    public void delete(UUID callerId, boolean isAdmin, Long activityId) {
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

        List<Carpool> activeCarpools = carpoolRepository.findAllActiveByActivityId(activityId);
        if (!activeCarpools.isEmpty()) {
            List<Long> carpoolIds = activeCarpools.stream().map(Carpool::getId).toList();
            carpoolPassengerRepository.removeAllByCarpoolIds(carpoolIds);
            carpoolRepository.cancelAllByActivityId(activityId);
        }

        subscriptionRepository.deleteAllByActivityId(activityId);
        activityRepository.softDelete(activityId);
    }

    @Transactional(readOnly = true)
    public List<ParticipantDto> getParticipants(Long activityId) {
        List<UUID> userIds = subscriptionRepository.findUserIdsByActivityId(activityId);
        return userIds.stream()
                .filter(Objects::nonNull)
                .map(uid -> userRepository.findById(uid)
                        .map(u -> new ParticipantDto(u.getId(), u.getFirstName(), u.getLastName(), u.getEmail()))
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ActivityResponseDto> getAllActivities(boolean includeDeleted) {
        List<Activity> activities = activityRepository.findAll();
        if (!includeDeleted) {
            activities = activities.stream().filter(a -> !a.isDeleted()).toList();
        }
        return toResponseList(activities);
    }

    @Transactional(readOnly = true)
    public List<ActivityResponseDto> getMyActivities(UUID userId) {
        List<Activity> activities = activityRepository.findByOrganizerId(userId);
        return toResponseList(activities);
    }

    @Transactional(readOnly = true)
    public List<ActivityResponseDto> getRegisteredActivities(UUID userId) {
        List<Activity> activities = activityRepository.findSubscribedAsNonOrganizer(userId);
        return toResponseList(activities);
    }

    @Transactional(readOnly = true)
    public List<ActivityResponseDto> getAvailableActivities(UUID userId) {
        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);
        List<Activity> activities = activityRepository.findAvailableForUser(userId, today, now);
        // Garantie métier : jamais les activités dont l'utilisateur connecté est l'organisateur
        // (la requête l'applique déjà ; filtre défensif si données incohérentes).
        List<Activity> withoutOwn = activities.stream()
                .filter(a -> !Objects.equals(a.getOrganizerId(), userId))
                .toList();
        return toResponseList(withoutOwn);
    }

    @Transactional
    public ActivityResponseDto subscribe(UUID userId, Long activityId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activité non trouvée"));
        if (activity.isDeleted()) {
            throw new ResourceNotFoundException("Activité non trouvée");
        }

        if (activity.getOrganizerId().equals(userId)) {
            throw new BusinessException("Vous ne pouvez pas vous inscrire à votre propre activité");
        }

        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);
        ActivitySubscriptionPolicy.validateActivityIsOpenForSubscription(activity, today, now);

        if (subscriptionRepository.existsByActivityIdAndUserId(activityId, userId)) {
            throw new ConflictException("Vous êtes déjà inscrit à cette activité");
        }

        int participantCount = subscriptionRepository.countParticipants(activityId);
        if (participantCount >= activity.getCapacity()) {
            throw new ConflictException("Capacité maximale atteinte pour cette activité");
        }

        boolean conflictAsOrganizer = activityRepository.existsOverlappingForUsersAsOrganizer(
                List.of(userId),
                activity.getDate(),
                activity.getStartTime(),
                activity.getEndTime(),
                activityId
        );
        boolean conflictAsParticipant = subscriptionRepository.existsConflictingActivityForSubscribedUsers(
                List.of(userId),
                activity.getDate(),
                activity.getStartTime(),
                activity.getEndTime(),
                activityId
        );

        if (conflictAsOrganizer || conflictAsParticipant) {
            throw new ConflictException("Cette activité entre en conflit avec une autre activité de votre planning");
        }

        subscriptionRepository.registerParticipant(activityId, userId);

        ActivityType type = activityTypeRepository.findByIdIncludingDeleted(activity.getTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Type d'activité non trouvé"));
        String organizerName = userRepository.findById(activity.getOrganizerId())
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse("Inconnu");
        int updatedParticipantCount = subscriptionRepository.countParticipants(activityId);
        CarpoolResponseDto carpoolResponse = carpoolRepository.findByActivityId(activityId)
                .map(this::toCarpoolResponse)
                .orElse(null);
        return toResponse(activity, type, organizerName, updatedParticipantCount, carpoolResponse);
    }

    @Transactional
    public ActivityResponseDto unsubscribe(UUID userId, Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activité non trouvée"));
        if (activity.isDeleted()) {
            throw new ResourceNotFoundException("Activité non trouvée");
        }

        if (activity.getOrganizerId().equals(userId)) {
            throw new BusinessException(
                    "Vous ne pouvez pas vous désinscrire en tant qu'organisateur de votre propre activité");
        }

        if (!subscriptionRepository.existsByActivityIdAndUserId(activityId, userId)) {
            throw new BusinessException("Vous n'êtes pas inscrit à cette activité");
        }

        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);
        ActivitySubscriptionPolicy.validateActivityAllowsUnsubscribe(activity, today, now);

        // Cleanup carpool: remove as passenger or cancel as driver
        carpoolRepository.findActiveByDriverIdAndActivityId(userId, activityId).ifPresent(driverCarpool -> {
            carpoolPassengerRepository.removeAllByCarpoolId(driverCarpool.getId());
            carpoolRepository.cancelByDriverIdAndActivityId(userId, activityId);
        });

        List<Carpool> activeCarpools = carpoolRepository.findAllActiveByActivityId(activityId);
        List<Long> carpoolIds = activeCarpools.stream().map(Carpool::getId).toList();
        carpoolPassengerRepository.findActiveByPassengerIdAndCarpoolIds(userId, carpoolIds)
                .ifPresent(cp -> carpoolPassengerRepository.removePassenger(cp.getCarpoolId(), userId));

        try {
            subscriptionRepository.unsubscribeParticipant(activityId, userId);
        } catch (IllegalStateException ex) {
            throw new BusinessException("Impossible de finaliser la désinscription");
        }

        ActivityType type = activityTypeRepository.findByIdIncludingDeleted(activity.getTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Type d'activité non trouvé"));
        String organizerName = userRepository.findById(activity.getOrganizerId())
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse("Inconnu");
        int updatedParticipantCount = subscriptionRepository.countParticipants(activityId);
        CarpoolResponseDto carpoolResponse = carpoolRepository.findByActivityId(activityId)
                .map(this::toCarpoolResponse)
                .orElse(null);
        return toResponse(activity, type, organizerName, updatedParticipantCount, carpoolResponse);
    }

    private List<ActivityResponseDto> toResponseList(List<Activity> activities) {
        Map<Long, ActivityType> typeCache = new HashMap<>();
        Map<UUID, String> organizerCache = new HashMap<>();
        return activities.stream()
                .map(activity -> {
                    ActivityType type = typeCache.computeIfAbsent(
                            activity.getTypeId(),
                            id -> activityTypeRepository.findByIdIncludingDeleted(id)
                                    .orElseThrow(() -> new ResourceNotFoundException("Type d'activité non trouvé"))
                    );
                    String organizerName = organizerCache.computeIfAbsent(
                            activity.getOrganizerId(),
                            id -> userRepository.findById(id)
                                    .map(u -> u.getFirstName() + " " + u.getLastName())
                                    .orElse("Inconnu")
                    );
                    int participantCount = subscriptionRepository.countParticipants(activity.getId());
                    CarpoolResponseDto carpoolResponse = carpoolRepository.findByActivityId(activity.getId())
                            .map(this::toCarpoolResponse)
                            .orElse(null);
                    return toResponse(activity, type, organizerName, participantCount, carpoolResponse);
                })
                .toList();
    }

    private ActivityResponseDto toResponse(Activity activity, ActivityType type, String organizerName,
                                            int participantCount, CarpoolResponseDto carpool) {
        LocationDto locationDto = new LocationDto(
                activity.getLocation().getRoom(),
                activity.getLocation().getStreet(),
                activity.getLocation().getComplement(),
                activity.getLocation().getPostalCode(),
                activity.getLocation().getCity()
        );
        LocationType locationType = activity.getLocationType() != null
                ? activity.getLocationType()
                : LocationType.OFF_SITE;
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
                organizerName,
                activity.isDeleted(),
                locationType,
                carpool
        );
    }
}
