package com.example.api.carpool.application;

import com.example.api.activity.domain.Activity;
import com.example.api.activity.domain.ActivityRepositoryPort;
import com.example.api.activity.domain.LocationType;
import com.example.api.carpool.application.dto.ActivityCarpoolsResponseDto;
import com.example.api.carpool.application.dto.CarpoolDetailDto;
import com.example.api.carpool.application.dto.CarpoolRequestDto;
import com.example.api.carpool.domain.Carpool;
import com.example.api.carpool.domain.CarpoolCreationPolicy;
import com.example.api.carpool.domain.CarpoolJoinPolicy;
import com.example.api.carpool.domain.CarpoolPassenger;
import com.example.api.carpool.domain.CarpoolPassengerRepositoryPort;
import com.example.api.carpool.domain.CarpoolRepositoryPort;
import com.example.api.carpool.domain.CarpoolStatus;
import com.example.api.shared.exception.BusinessException;
import com.example.api.shared.exception.ResourceNotFoundException;
import com.example.api.subscription.domain.SubscriptionRepositoryPort;
import com.example.api.user.domain.UserRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class CarpoolUseCase {

    private final CarpoolRepositoryPort carpoolRepository;
    private final CarpoolPassengerRepositoryPort carpoolPassengerRepository;
    private final ActivityRepositoryPort activityRepository;
    private final SubscriptionRepositoryPort subscriptionRepository;
    private final UserRepositoryPort userRepository;
    private final Clock clock;

    public CarpoolUseCase(
            CarpoolRepositoryPort carpoolRepository,
            CarpoolPassengerRepositoryPort carpoolPassengerRepository,
            ActivityRepositoryPort activityRepository,
            SubscriptionRepositoryPort subscriptionRepository,
            UserRepositoryPort userRepository,
            Clock clock) {
        this.carpoolRepository = carpoolRepository;
        this.carpoolPassengerRepository = carpoolPassengerRepository;
        this.activityRepository = activityRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    /* ────────────────────────────── List ─────────────────────────────────── */

    @Transactional(readOnly = true)
    public ActivityCarpoolsResponseDto listCarpools(Long activityId, Long currentUserId) {
        Activity activity = findValidActivity(activityId);
        requireOffSite(activity);

        List<Carpool> carpools = carpoolRepository.findAllActiveByActivityId(activityId);
        List<Long> carpoolIds = carpools.stream().map(Carpool::getId).toList();

        // Determine user's role
        String userRole = "NONE";
        Long userCarpoolId = null;

        boolean isDriver = carpools.stream().anyMatch(c -> c.getDriverId().equals(currentUserId));
        if (isDriver) {
            userRole = "DRIVER";
            userCarpoolId = carpools.stream()
                    .filter(c -> c.getDriverId().equals(currentUserId))
                    .findFirst()
                    .map(Carpool::getId)
                    .orElse(null);
        } else if (!carpoolIds.isEmpty()) {
            var passengerEntry = carpoolPassengerRepository
                    .findActiveByPassengerIdAndCarpoolIds(currentUserId, carpoolIds);
            if (passengerEntry.isPresent()) {
                userRole = "PASSENGER";
                userCarpoolId = passengerEntry.get().getCarpoolId();
            }
        }

        List<CarpoolDetailDto> details = carpools.stream()
                .map(c -> toDetail(c, carpoolPassengerRepository.countActive(c.getId())))
                .toList();

        return new ActivityCarpoolsResponseDto(details, userRole, userCarpoolId);
    }

    /* ──────────────────────────── Create ─────────────────────────────────── */

    @Transactional
    public CarpoolDetailDto createCarpool(Long activityId, Long userId, CarpoolRequestDto dto) {
        Activity activity = findValidActivity(activityId);
        requireOffSite(activity);
        requireActivityNotPast(activity);

        if (!subscriptionRepository.existsByActivityIdAndUserId(activityId, userId)) {
            throw new BusinessException("Vous devez être inscrit à l'activité pour proposer un covoiturage");
        }

        // Check user has no existing carpool role for this activity
        requireNoExistingCarpoolRole(activityId, userId);

        CarpoolCreationPolicy.validate(dto.maxPassengers(), dto.departureTime(), activity.getStartTime());

        Carpool carpool = Carpool.builder()
                .activityId(activityId)
                .driverId(userId)
                .departureTime(dto.departureTime())
                .maxPassengers(dto.maxPassengers())
                .status(CarpoolStatus.ACTIVE)
                .build();

        Carpool saved = carpoolRepository.save(carpool);
        return toDetail(saved, 0);
    }

    /* ───────────────────────────── Join ──────────────────────────────────── */

    @Transactional
    public CarpoolDetailDto joinCarpool(Long activityId, Long carpoolId, Long userId) {
        Activity activity = findValidActivity(activityId);
        requireOffSite(activity);
        requireActivityNotPast(activity);

        if (!subscriptionRepository.existsByActivityIdAndUserId(activityId, userId)) {
            throw new BusinessException("Vous devez être inscrit à l'activité pour rejoindre un covoiturage");
        }

        Carpool carpool = carpoolRepository.findById(carpoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Covoiturage non trouvé"));

        if (!carpool.getActivityId().equals(activityId)) {
            throw new ResourceNotFoundException("Covoiturage non trouvé pour cette activité");
        }

        boolean userAlreadyHasCarpoolRole = userAlreadyHasCarpoolRole(activityId, userId);
        int passengerCount = carpoolPassengerRepository.countActive(carpoolId);

        CarpoolJoinPolicy.validate(carpool, userId, userAlreadyHasCarpoolRole, passengerCount);

        CarpoolPassenger passenger = CarpoolPassenger.builder()
                .carpoolId(carpoolId)
                .passengerId(userId)
                .joinedAt(LocalDateTime.now(clock))
                .build();
        carpoolPassengerRepository.save(passenger);

        return toDetail(carpool, passengerCount + 1);
    }

    /* ───────────────────────────── Leave ─────────────────────────────────── */

    @Transactional
    public void leaveCarpool(Long activityId, Long carpoolId, Long userId) {
        Activity activity = findValidActivity(activityId);
        requireActivityNotPast(activity);

        Carpool carpool = carpoolRepository.findById(carpoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Covoiturage non trouvé"));

        if (!carpool.getActivityId().equals(activityId)) {
            throw new ResourceNotFoundException("Covoiturage non trouvé pour cette activité");
        }

        if (!carpoolPassengerRepository.existsActive(carpoolId, userId)) {
            throw new BusinessException("Vous n'êtes pas passager de ce covoiturage");
        }

        carpoolPassengerRepository.removePassenger(carpoolId, userId);
    }

    /* ─────────────────────────── Helpers ─────────────────────────────────── */

    private Activity findValidActivity(Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activité non trouvée"));
        if (activity.isDeleted()) {
            throw new ResourceNotFoundException("Activité non trouvée");
        }
        return activity;
    }

    private void requireOffSite(Activity activity) {
        if (activity.getLocationType() == LocationType.ON_SITE) {
            throw new BusinessException("Le covoiturage n'est disponible que pour les activités hors site");
        }
    }

    private void requireActivityNotPast(Activity activity) {
        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);
        if (activity.getDate().isBefore(today) ||
                (activity.getDate().isEqual(today) && !now.isBefore(activity.getStartTime()))) {
            throw new BusinessException("L'activité est déjà passée ou en cours");
        }
    }

    private void requireNoExistingCarpoolRole(Long activityId, Long userId) {
        if (userAlreadyHasCarpoolRole(activityId, userId)) {
            throw new BusinessException("Vous avez déjà un rôle de covoiturage pour cette activité");
        }
    }

    private boolean userAlreadyHasCarpoolRole(Long activityId, Long userId) {
        boolean isDriver = carpoolRepository.findActiveByDriverIdAndActivityId(userId, activityId).isPresent();
        if (isDriver) return true;
        List<Carpool> activeCarpools = carpoolRepository.findAllActiveByActivityId(activityId);
        List<Long> ids = activeCarpools.stream().map(Carpool::getId).toList();
        return !ids.isEmpty() &&
                carpoolPassengerRepository.findActiveByPassengerIdAndCarpoolIds(userId, ids).isPresent();
    }

    private CarpoolDetailDto toDetail(Carpool carpool, int passengerCount) {
        String driverName = userRepository.findById(carpool.getDriverId())
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse("Inconnu");
        int availableSeats = Math.max(0, carpool.getMaxPassengers() - passengerCount);
        return new CarpoolDetailDto(
                carpool.getId(),
                carpool.getActivityId(),
                carpool.getDriverId(),
                driverName,
                carpool.getDepartureTime(),
                carpool.getMaxPassengers(),
                passengerCount,
                availableSeats,
                carpool.getStatus()
        );
    }
}
