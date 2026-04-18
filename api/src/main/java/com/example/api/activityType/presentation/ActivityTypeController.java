package com.example.api.activityType.presentation;

import com.example.api.activityType.application.ActivityTypeUseCase;
import com.example.api.activityType.application.dto.ActivityTypeCountDto;
import com.example.api.activityType.application.dto.ActivityTypeRequestDto;
import com.example.api.activityType.application.dto.ActivityTypeResponseDto;
import com.example.api.shared.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @GetMapping("/count")
    @Operation(summary = "Nombre de types d'activité", description = "Retourne le nombre total de types (requête légère côté base).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nombre retourné"),
            @ApiResponse(responseCode = "401", description = "Authentification requise")
    })
    public ResponseEntity<ActivityTypeCountDto> count() {
        return ResponseEntity.ok(activityTypeUseCase.count());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un type d'activité", description = "Retourne un type d'activité par son identifiant.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Type d'activité retourné"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "404", description = "Type d'activité non trouvé")
    })
    public ResponseEntity<ActivityTypeResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(activityTypeUseCase.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer un type d'activité (admin)", description = "Crée un nouveau type d'activité.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Type d'activité créé"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "403", description = "Réservé aux admins"),
            @ApiResponse(responseCode = "409", description = "Type d'activité déjà existant")
    })
    public ResponseEntity<ActivityTypeResponseDto> create(@Valid @RequestBody ActivityTypeRequestDto dto) {
        ActivityTypeResponseDto body = activityTypeUseCase.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier un type d'activité (admin)", description = "Modifie le nom d'un type d'activité.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Type d'activité modifié"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "403", description = "Réservé aux admins"),
            @ApiResponse(responseCode = "404", description = "Type d'activité non trouvé"),
            @ApiResponse(responseCode = "409", description = "Type d'activité déjà existant")
    })
    public ResponseEntity<ActivityTypeResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody ActivityTypeRequestDto dto) {
        return ResponseEntity.ok(activityTypeUseCase.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un type d'activité (admin)", description = "Supprime un type d'activité.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Type d'activité supprimé"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "403", description = "Réservé aux admins"),
            @ApiResponse(responseCode = "404", description = "Type d'activité non trouvé")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        activityTypeUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
