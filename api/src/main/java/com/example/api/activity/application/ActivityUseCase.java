package com.example.api.activity.application;

import com.example.api.activity.application.dto.ActivityResponseDto;
import com.example.api.activity.application.dto.ActivityTypeDto;
import com.example.api.activity.application.dto.CreateActivityRequestDto;
import com.example.api.activity.application.dto.LocationDto;
import com.example.api.activity.domain.Activity;
import com.example.api.activity.domain.ActivityCreationPolicy;
import com.example.api.activity.domain.ActivityRepositoryPort;
import com.example.api.activity.domain.Location;
import com.example.api.activityType.domain.ActivityType;
import com.example.api.activityType.domain.ActivityTypeRepositoryPort;
import com.example.api.shared.exception.ConflictException;
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

        return toResponse(saved, activityType);
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
        return activities.stream()
                .map(activity -> {
                    ActivityType type = typeCache.computeIfAbsent(
                            activity.getTypeId(),
                            id -> activityTypeRepository.findById(id)
                                    .orElseThrow(() -> new ResourceNotFoundException("Type d'activité non trouvé"))
                    );
                    return toResponse(activity, type);
                })
                .toList();
    }

    private ActivityResponseDto toResponse(Activity activity, ActivityType type) {
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
                locationDto,
                typeDto,
                activity.getDate(),
                activity.getStartTime(),
                activity.getEndTime()
        );
    }
}
