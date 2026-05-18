package com.example.api.carpool.domain;

import java.util.Optional;

public interface CarpoolRepositoryPort {
    Carpool save(Carpool carpool);

    Optional<Carpool> findByActivityId(Long activityId);
}
