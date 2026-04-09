package com.example.api.activityType.presentation;

import com.example.api.activityType.application.ActivityTypeUseCase;
import com.example.api.activityType.application.dto.ActivityTypeResponseDto;
import com.example.api.shared.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/activity-types")
@Tag(name = "Types d'activité", description = "Référentiel des types d'activité.")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class ActivityTypeController {

    private final ActivityTypeUseCase activityTypeUseCase;

    public ActivityTypeController(ActivityTypeUseCase activityTypeUseCase) {
        this.activityTypeUseCase = activityTypeUseCase;
    }

    @GetMapping
    @Operation(summary = "Lister les types d'activité", description = "Retourne tous les types d'activité disponibles.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste retournée"),
            @ApiResponse(responseCode = "401", description = "Authentification requise")
    })
    public ResponseEntity<List<ActivityTypeResponseDto>> findAll() {
        return ResponseEntity.ok(activityTypeUseCase.findAll());
    }
}
