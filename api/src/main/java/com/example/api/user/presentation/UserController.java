package com.example.api.user.presentation;

import com.example.api.shared.openapi.OpenApiConfig;
import com.example.api.shared.security.JwtPrincipal;
import com.example.api.user.application.UserUseCase;
import com.example.api.user.application.dto.ChangePasswordRequestDto;
import com.example.api.user.application.dto.UpdateProfileRequestDto;
import com.example.api.user.application.dto.UserResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Tag(name = "Utilisateurs", description = "Consultation et mise à jour du profil utilisateur.")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class UserController {

    private final UserUseCase userUseCase;

    public UserController(UserUseCase userUseCase) {
        this.userUseCase = userUseCase;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Liste tous les utilisateurs (admin)",
            description = "Retourne la liste complète des utilisateurs triée par date d'inscription décroissante. Réservé aux administrateurs."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste retournée"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "403", description = "Droits admin requis")
    })
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(userUseCase.getAllUsers());
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Nombre total d'utilisateurs (admin)",
            description = "Retourne le nombre total d'utilisateurs enregistrés. Réservé aux administrateurs."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comptage retourné"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "403", description = "Droits admin requis")
    })
    public ResponseEntity<Map<String, Long>> countUsers() {
        return ResponseEntity.ok(Map.of("count", userUseCase.countUsers()));
    }

    /**
     * GET /user/{id}
     * Un COLLABORATOR ne peut accéder qu'à son propre profil.
     * Un ADMIN peut accéder à n'importe quel profil.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.userId")
    @Operation(summary = "Récupérer un utilisateur", description = "Retourne le profil utilisateur. Un COLLABORATOR ne peut accéder qu'à son propre profil, un ADMIN peut accéder à tous.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil retourné"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "403", description = "Accès refusé (si non autorisé)"),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userUseCase.getUserById(id));
    }

    /**
     * PATCH /user/{id}/password
     * Modifie le mot de passe après vérification du mot de passe actuel.
     * Accessible uniquement par l'utilisateur lui-même.
     */
    @PatchMapping("/{id}/password")
    @PreAuthorize("#id == authentication.principal.userId")
    @Operation(summary = "Changer le mot de passe", description = "Modifie le mot de passe de l'utilisateur après vérification du mot de passe actuel. Accessible uniquement par l'utilisateur lui-même.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Mot de passe modifié"),
            @ApiResponse(responseCode = "400", description = "Nouveau mot de passe invalide"),
            @ApiResponse(responseCode = "401", description = "Mot de passe actuel incorrect ou non authentifié"),
            @ApiResponse(responseCode = "403", description = "Accès refusé"),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    public ResponseEntity<Void> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequestDto request) {
        userUseCase.changePassword(id, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /user/{id}/profile
     * Modifie uniquement bio, phone et address.
     * Un COLLABORATOR ne peut modifier que son propre profil.
     * Un ADMIN peut modifier n'importe quel profil.
     */
    @PatchMapping("/{id}/profile")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.userId")
    @Operation(summary = "Mettre à jour le profil", description = "Met à jour uniquement bio, phone et address. Un COLLABORATOR ne peut modifier que son propre profil, un ADMIN peut modifier tous.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil mis à jour"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "403", description = "Accès refusé (si non autorisé)"),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    public ResponseEntity<UserResponseDto> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProfileRequestDto request,
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(userUseCase.updateProfile(id, request));
    }
}
