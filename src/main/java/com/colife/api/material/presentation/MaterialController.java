package com.colife.api.material.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import com.colife.api.material.application.MaterialUseCase;
import com.colife.api.material.application.dto.MaterialRequestDto;
import com.colife.api.material.application.dto.MaterialResponseDto;
import com.colife.api.shared.openapi.OpenApiConfig;
import com.colife.api.shared.security.JwtPrincipal;

import java.util.List;

@RestController
@RequestMapping("/activities/{activityId}/materials")
@Tag(name = "Matériel", description = "Propositions de matériel apporté par les participants pour une activité.")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class MaterialController {

    private final MaterialUseCase materialUseCase;

    public MaterialController(MaterialUseCase materialUseCase) {
        this.materialUseCase = materialUseCase;
    }

    @GetMapping
    @Operation(
            summary = "Lister le matériel proposé pour une activité",
            description = "Retourne toutes les propositions de matériel liées à l'activité."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste retournée"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "404", description = "Activité non trouvée")
    })
    public ResponseEntity<List<MaterialResponseDto>> list(
            @PathVariable Long activityId,
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(materialUseCase.listMaterials(activityId, principal.userId()));
    }

    @PostMapping
    @Operation(
            summary = "Proposer du matériel",
            description = "Propose un objet à apporter pour l'activité. L'utilisateur doit être inscrit ou organisateur."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Proposition créée"),
            @ApiResponse(responseCode = "400", description = "Règle métier non respectée"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "404", description = "Activité non trouvée")
    })
    public ResponseEntity<MaterialResponseDto> propose(
            @PathVariable Long activityId,
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody MaterialRequestDto dto) {
        MaterialResponseDto result = materialUseCase.proposeMaterial(activityId, principal.userId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/{materialId}")
    @Operation(
            summary = "Modifier une proposition de matériel",
            description = "Seul l'auteur de la proposition peut la modifier."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proposition mise à jour"),
            @ApiResponse(responseCode = "400", description = "Règle métier non respectée"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "403", description = "Non autorisé"),
            @ApiResponse(responseCode = "404", description = "Proposition non trouvée")
    })
    public ResponseEntity<MaterialResponseDto> update(
            @PathVariable Long activityId,
            @PathVariable Long materialId,
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody MaterialRequestDto dto) {
        MaterialResponseDto result = materialUseCase.updateMaterial(activityId, materialId, principal.userId(), dto);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{materialId}")
    @Operation(
            summary = "Retirer une proposition de matériel",
            description = "Seul l'auteur de la proposition peut la retirer."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Proposition retirée"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "403", description = "Non autorisé"),
            @ApiResponse(responseCode = "404", description = "Proposition non trouvée")
    })
    public ResponseEntity<Void> remove(
            @PathVariable Long activityId,
            @PathVariable Long materialId,
            @AuthenticationPrincipal JwtPrincipal principal) {
        materialUseCase.removeMaterial(activityId, materialId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
