package com.example.api.activity.domain;

import java.util.List;
import java.util.Optional;

public interface ActivityRepositoryPort {
    Optional<Activity> findById(Long id);
    Activity save(Activity activity);
    List<Activity> findAll();
}
