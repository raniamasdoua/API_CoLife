package com.example.api.activity.presentation;

import com.example.api.activity.application.ActivityUseCase;
import com.example.api.activity.application.dto.ActivityResponseDto;
import com.example.api.activity.application.dto.CreateActivityRequestDto;
import com.example.api.shared.security.JwtPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/activities")
public class ActivityController {

    private final ActivityUseCase activityUseCase;

    public ActivityController(ActivityUseCase activityUseCase) {
        this.activityUseCase = activityUseCase;
    }

    @PostMapping
    public ResponseEntity<ActivityResponseDto> create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateActivityRequestDto dto) {
        ActivityResponseDto body = activityUseCase.create(principal.userId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
