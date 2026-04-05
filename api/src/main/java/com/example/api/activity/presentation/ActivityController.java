package com.example.api.activity.presentation;

import com.example.api.activity.application.ActivityUseCase;
import com.example.api.activity.application.dto.ActivityResponseDto;
import com.example.api.activity.application.dto.CreateActivityRequestDto;
import com.example.api.activity.application.dto.UpdateActivityRequestDto;
import com.example.api.shared.security.JwtPrincipal;
import com.example.api.user.domain.Role;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @PutMapping("/{activityId}")
    public ResponseEntity<ActivityResponseDto> update(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long activityId,
            @Valid @RequestBody UpdateActivityRequestDto dto) {
        boolean isAdmin = principal.role() == Role.ADMIN;
        ActivityResponseDto body = activityUseCase.update(principal.userId(), isAdmin, activityId, dto);
        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/{activityId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long activityId) {
        boolean isAdmin = principal.role() == Role.ADMIN;
        activityUseCase.delete(principal.userId(), isAdmin, activityId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ActivityResponseDto>> getMyActivities(
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(activityUseCase.getMyActivities(principal.userId()));
    }

    @GetMapping("/available")
    public ResponseEntity<List<ActivityResponseDto>> getAvailableActivities(
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(activityUseCase.getAvailableActivities(principal.userId()));
    }

}
