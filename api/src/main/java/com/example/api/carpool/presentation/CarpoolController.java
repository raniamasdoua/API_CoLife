package com.example.api.carpool.presentation;

import com.example.api.carpool.application.CarpoolUseCase;
import com.example.api.carpool.application.dto.ActivityCarpoolsResponseDto;
import com.example.api.carpool.application.dto.CarpoolDetailDto;
import com.example.api.carpool.application.dto.CarpoolRequestDto;
import com.example.api.shared.openapi.OpenApiConfig;
import com.example.api.shared.security.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/activities/{activityId}/carpools")
@Tag(name = "Covoiturage", description = "Gestion des propositions de covoiturage liées aux activités hors site.")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class CarpoolController {

    private final CarpoolUseCase carpoolUseCase;

    public CarpoolController(CarpoolUseCase carpoolUseCase) {
        this.carpoolUseCase = carpoolUseCase;
    }

    @GetMapping
    @Operation(
            summary = "Lister les covoiturages d'une activité",
            description = "Retourne les covoiturages actifs avec le rôle de l'utilisateur connecté (DRIVER, PASSENGER ou NONE)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste retournée"),
            @ApiResponse(responseCode = "400", description = "Activité sur site — covoiturage non disponible"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "404", description = "Activité non trouvée")
    })
    public ResponseEntity<ActivityCarpoolsResponseDto> list(
            @PathVariable Long activityId,
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(carpoolUseCase.listCarpools(activityId, principal.userId()));
    }

    @PostMapping
    @Operation(
            summary = "Proposer un covoiturage",
            description = "Crée une proposition de covoiturage pour l'activité. L'utilisateur doit être inscrit et ne pas déjà avoir un rôle de covoiturage."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Covoiturage créé"),
            @ApiResponse(responseCode = "400", description = "Règle métier non respectée"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "404", description = "Activité non trouvée")
    })
    public ResponseEntity<CarpoolDetailDto> create(
            @PathVariable Long activityId,
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CarpoolRequestDto dto) {
        CarpoolDetailDto result = carpoolUseCase.createCarpool(activityId, principal.userId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/{carpoolId}/join")
    @Operation(
            summary = "Rejoindre un covoiturage",
            description = "Rejoint une proposition de covoiturage existante en tant que passager."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inscription au covoiturage effectuée"),
            @ApiResponse(responseCode = "400", description = "Règle métier non respectée (complet, déjà inscrit, etc.)"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "404", description = "Activité ou covoiturage non trouvé")
    })
    public ResponseEntity<CarpoolDetailDto> join(
            @PathVariable Long activityId,
            @PathVariable Long carpoolId,
            @AuthenticationPrincipal JwtPrincipal principal) {
        CarpoolDetailDto result = carpoolUseCase.joinCarpool(activityId, carpoolId, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @DeleteMapping("/{carpoolId}/leave")
    @Operation(
            summary = "Quitter un covoiturage",
            description = "Retire l'utilisateur d'une proposition de covoiturage dont il est passager."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Désinscription du covoiturage effectuée"),
            @ApiResponse(responseCode = "400", description = "Règle métier non respectée"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "404", description = "Covoiturage non trouvé")
    })
    public ResponseEntity<Void> leave(
            @PathVariable Long activityId,
            @PathVariable Long carpoolId,
            @AuthenticationPrincipal JwtPrincipal principal) {
        carpoolUseCase.leaveCarpool(activityId, carpoolId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
