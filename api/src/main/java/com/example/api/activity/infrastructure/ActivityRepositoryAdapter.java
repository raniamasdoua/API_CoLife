package com.example.api.activity.infrastructure;

import com.example.api.activity.domain.Activity;
import com.example.api.activity.domain.ActivityMapper;
import com.example.api.activity.domain.ActivityRepositoryPort;
import com.example.api.shared.exception.ResourceNotFoundException;
import com.example.api.activityType.infrastructure.ActivityTypeJpaRepository;
import com.example.api.user.infrastructure.UserJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Component
public class ActivityRepositoryAdapter implements ActivityRepositoryPort {

    private final ActivityJpaRepository activityJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final ActivityTypeJpaRepository activityTypeJpaRepository;

    public ActivityRepositoryAdapter(
            ActivityJpaRepository activityJpaRepository,
            UserJpaRepository userJpaRepository,
            ActivityTypeJpaRepository activityTypeJpaRepository) {
        this.activityJpaRepository = activityJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.activityTypeJpaRepository = activityTypeJpaRepository;
    }

    @Override
    public Optional<Activity> findById(Long id) {
        return activityJpaRepository.findById(id).map(ActivityMapper::toDomain);
    }

    @Override
    public Activity save(Activity activity) {
        ActivityEntity entity = new ActivityEntity();
        entity.setTitle(activity.getTitle());
        entity.setDescription(activity.getDescription());
        entity.setCapacity(activity.getCapacity());
        entity.setLocation(ActivityMapper.toEmbeddable(activity.getLocation()));
        entity.setOrganizer(userJpaRepository.getReferenceById(activity.getOrganizerId()));
        entity.setType(activityTypeJpaRepository.getReferenceById(activity.getTypeId()));
        entity.setDate(activity.getDate());
        entity.setStartTime(activity.getStartTime());
        entity.setEndTime(activity.getEndTime());
        entity.setDeleted(false);
        ActivityEntity saved = activityJpaRepository.save(entity);
        return ActivityMapper.toDomain(saved);
    }

    @Override
    public List<Activity> findAll() {
        return activityJpaRepository.findAll().stream()
                .map(ActivityMapper::toDomain)
                .toList();
    }

    @Override
    public List<Activity> findByOrganizerId(Long organizerId) {
        return activityJpaRepository.findByOrganizerIdAndNotDeleted(organizerId).stream()
                .map(ActivityMapper::toDomain)
                .toList();
    }

    @Override
    public List<Activity> findByOrganizerIdNot(Long organizerId) {
        return activityJpaRepository.findByOrganizerIdNotAndNotDeleted(organizerId).stream()
                .map(ActivityMapper::toDomain)
                .toList();
    }

    @Override
    public List<Activity> findAvailableForUser(Long userId, LocalDate today, LocalTime now) {
        return activityJpaRepository.findAvailableForUser(userId, today, now).stream()
                .map(ActivityMapper::toDomain)
                .toList();
    }

    @Override
    public Activity update(Activity activity) {
        ActivityEntity entity = activityJpaRepository.findById(activity.getId())
                .orElseThrow(() -> new RuntimeException("Activité non trouvée lors de la mise à jour"));
        entity.setTitle(activity.getTitle());
        entity.setDescription(activity.getDescription());
        entity.setCapacity(activity.getCapacity());
        entity.setLocation(ActivityMapper.toEmbeddable(activity.getLocation()));
        entity.setType(activityTypeJpaRepository.getReferenceById(activity.getTypeId()));
        entity.setDate(activity.getDate());
        entity.setStartTime(activity.getStartTime());
        entity.setEndTime(activity.getEndTime());
        ActivityEntity saved = activityJpaRepository.save(entity);
        return ActivityMapper.toDomain(saved);
    }

    @Override
    public boolean existsOverlappingForOrganizer(Long organizerId, LocalDate date, LocalTime start, LocalTime end) {
        return activityJpaRepository.existsOverlappingForOrganizer(organizerId, date, start, end);
    }

    @Override
    public boolean existsOverlappingForUsersAsOrganizer(List<Long> userIds, LocalDate date, LocalTime start, LocalTime end, Long excludeActivityId) {
        if (userIds == null || userIds.isEmpty()) {
            return false;
        }
        return activityJpaRepository.existsOverlappingForUsersAsOrganizer(userIds, date, start, end, excludeActivityId);
    }

    @Override
    public void softDelete(Long activityId) {
        ActivityEntity entity = activityJpaRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activité non trouvée"));
        entity.setDeleted(true);
        activityJpaRepository.save(entity);
    }

}
