package com.example.api.activityType.presentation;

import com.example.api.activityType.application.ActivityTypeUseCase;
import com.example.api.activityType.application.dto.ActivityTypeResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/activity-types")
public class ActivityTypeController {

    private final ActivityTypeUseCase activityTypeUseCase;

    public ActivityTypeController(ActivityTypeUseCase activityTypeUseCase) {
        this.activityTypeUseCase = activityTypeUseCase;
    }

    @GetMapping
    public ResponseEntity<List<ActivityTypeResponseDto>> findAll() {
        return ResponseEntity.ok(activityTypeUseCase.findAll());
    }
}
