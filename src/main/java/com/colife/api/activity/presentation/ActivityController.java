package com.colife.api.activity.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.colife.api.activity.application.ActivityUseCase;
import com.colife.api.activity.application.dto.ActivityResponseDto;
import com.colife.api.activity.application.dto.CreateActivityRequestDto;
import com.colife.api.activity.application.dto.ParticipantDto;
import com.colife.api.activity.application.dto.UpdateActivityRequestDto;
import com.colife.api.activity.application.dto.UserActivitiesDto;
import com.colife.api.shared.openapi.OpenApiConfig;
import com.colife.api.shared.security.JwtPrincipal;
import com.colife.api.user.domain.Role;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/activities")
@Tag(name = "Activités", description = "Gestion des activités (création, modification, suppression, consultation, inscription).")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class ActivityController {

    private final ActivityUseCase activityUseCase;

    public ActivityController(ActivityUseCase activityUseCase) {
        this.activityUseCase = activityUseCase;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Lister toutes les activités (admin)",
            description = "Retourne toutes les activités non supprimées. Réservé aux administrateurs."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste retournée"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "403", description = "Droits admin requis")
    })
    public ResponseEntity<List<ActivityResponseDto>> getAllActivities(
            @RequestParam(name = "includeDeleted", defaultValue = "false") boolean includeDeleted) {
        return ResponseEntity.ok(activityUseCase.getAllActivities(includeDeleted));
    }

    @GetMapping("/{activityId}/participants")
    @Operation(
            summary = "Lister les participants d'une activité",
            description = "Retourne la liste des membres inscrits à une activité. Accessible à tout utilisateur authentifié."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste retournée"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "404", description = "Activité non trouvée")
    })
    public ResponseEntity<List<ParticipantDto>> getParticipants(
            @PathVariable Long activityId) {
        return ResponseEntity.ok(activityUseCase.getParticipants(activityId));
    }

    @PostMapping
    @Operation(
            summary = "Créer une activité",
            description = "Crée une nouvelle activité. L'organisateur est automatiquement inscrit comme participant."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Activité créée"),
            @ApiResponse(responseCode = "400", description = "Données invalides ou règle métier non respectée"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "409", description = "Conflit de planning (créneau déjà occupé)")
    })
    public ResponseEntity<ActivityResponseDto> create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateActivityRequestDto dto) {
        ActivityResponseDto body = activityUseCase.create(principal.userId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PutMapping("/{activityId}")
    @Operation(
            summary = "Modifier une activité",
            description = "Modifie une activité existante (organisateur ou admin). Impossible si l'activité est passée ou en cours."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Activité modifiée"),
            @ApiResponse(responseCode = "400", description = "Données invalides ou règle métier non respectée"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "403", description = "Non autorisé (si non admin et non organisateur)"),
            @ApiResponse(responseCode = "404", description = "Activité non trouvée"),
            @ApiResponse(responseCode = "409", description = "Conflit de planning (organisateur/participants)")
    })
    public ResponseEntity<ActivityResponseDto> update(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long activityId,
            @Valid @RequestBody UpdateActivityRequestDto dto) {
        boolean isAdmin = principal.role() == Role.ADMIN;
        ActivityResponseDto body = activityUseCase.update(principal.userId(), isAdmin, activityId, dto);
        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/{activityId}")
    @Operation(
            summary = "Supprimer une activité",
            description = "Supprime une activité (soft delete). Organisateur ou admin uniquement. Impossible si passée/en cours."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Activité supprimée"),
            @ApiResponse(responseCode = "400", description = "Règle métier non respectée (activité passée/en cours)"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "403", description = "Non autorisé (si non admin et non organisateur)"),
            @ApiResponse(responseCode = "404", description = "Activité non trouvée")
    })
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long activityId) {
        boolean isAdmin = principal.role() == Role.ADMIN;
        activityUseCase.delete(principal.userId(), isAdmin, activityId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Lister les activités liées à un utilisateur (admin)",
            description = "Retourne les activités organisées par l'utilisateur et celles auxquelles il est inscrit. Réservé aux administrateurs."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste retournée"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "403", description = "Droits admin requis")
    })
    public ResponseEntity<UserActivitiesDto> getUserActivities(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(activityUseCase.getUserActivities(userId));
    }

    @GetMapping("/mine")
    @Operation(summary = "Lister mes activités", description = "Retourne les activités dont l'utilisateur est organisateur.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste retournée"),
            @ApiResponse(responseCode = "401", description = "Authentification requise")
    })
    public ResponseEntity<List<ActivityResponseDto>> getMyActivities(
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(activityUseCase.getMyActivities(principal.userId()));
    }

    @GetMapping("/registered")
    @Operation(
            summary = "Lister mes inscriptions (hors activités que j'organise)",
            description = "Retourne les activités auxquelles l'utilisateur est inscrit en tant que participant, "
                    + "sans celles dont il est l'organisateur."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste retournée"),
            @ApiResponse(responseCode = "401", description = "Authentification requise")
    })
    public ResponseEntity<List<ActivityResponseDto>> getRegisteredActivities(
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(activityUseCase.getRegisteredActivities(principal.userId()));
    }

    @GetMapping("/available")
    @Operation(
            summary = "Lister les activités disponibles",
            description = "Retourne les activités auxquelles l'utilisateur peut s'inscrire : "
                    + "non supprimées, à venir (pas passées / pas encore commencées), "
                    + "dont l'utilisateur connecté n'est pas l'organisateur, "
                    + "et auxquelles il n'est pas déjà inscrit."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste retournée"),
            @ApiResponse(responseCode = "401", description = "Authentification requise")
    })
    public ResponseEntity<List<ActivityResponseDto>> getAvailableActivities(
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(activityUseCase.getAvailableActivities(principal.userId()));
    }

    @PostMapping("/{activityId}/subscribe")
    @Operation(
            summary = "S'inscrire à une activité",
            description = "Inscrit l'utilisateur authentifié à une activité en respectant les règles: activité existante, non supprimée, ouverte à l'inscription, pas de double inscription, capacité, pas de conflit planning (participant ou organisateur)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inscription effectuée"),
            @ApiResponse(responseCode = "400", description = "Règle métier non respectée (activité passée/en cours, organisateur, etc.)"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "404", description = "Activité non trouvée"),
            @ApiResponse(responseCode = "409", description = "Double inscription, capacité atteinte, ou conflit de planning")
    })
    public ResponseEntity<ActivityResponseDto> subscribe(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long activityId) {
        ActivityResponseDto body = activityUseCase.subscribe(principal.userId(), activityId);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @DeleteMapping("/{activityId}/subscribe")
    @Operation(
            summary = "Se désinscrire d'une activité",
            description = "Retire l'utilisateur authentifié des participants (soft delete avec date de désinscription). "
                    + "Impossible pour l'organisateur, si non inscrit, ou si l'activité a déjà commencé / est passée."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Désinscription effectuée, détail activité avec effectif à jour"),
            @ApiResponse(responseCode = "400", description = "Règle métier non respectée"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "404", description = "Activité non trouvée")
    })
    public ResponseEntity<ActivityResponseDto> unsubscribe(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long activityId) {
        ActivityResponseDto body = activityUseCase.unsubscribe(principal.userId(), activityId);
        return ResponseEntity.ok(body);
    }

}
