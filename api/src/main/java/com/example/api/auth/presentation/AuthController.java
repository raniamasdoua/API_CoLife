package com.example.api.auth.presentation;

import com.example.api.auth.application.AuthUseCase;
import com.example.api.auth.application.dto.MeResponseDto;
import com.example.api.shared.openapi.OpenApiConfig;
import com.example.api.shared.security.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentification", description = "Informations du compte courant. "
        + "L'inscription et la connexion sont gérées par Keycloak (OIDC).")
public class AuthController {

    private final AuthUseCase useCase;

    public AuthController(AuthUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/me")
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    @Operation(summary = "Récupérer mon profil (me)", description = "Retourne les informations de l'utilisateur authentifié (profil local synchronisé depuis Keycloak).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Informations retournées"),
            @ApiResponse(responseCode = "401", description = "Authentification requise")
    })
    public ResponseEntity<MeResponseDto> me(@AuthenticationPrincipal JwtPrincipal principal) {
        MeResponseDto me = useCase.getMe(principal);
        return ResponseEntity.ok(me);
    }
}
