package com.example.api.auth.presentation;

import com.example.api.auth.application.AuthUseCase;
import com.example.api.auth.application.dto.LoginRequestDto;
import com.example.api.auth.application.dto.LoginResponseDto;
import com.example.api.auth.application.dto.MeResponseDto;
import com.example.api.auth.application.dto.RegisterRequestDto;
import com.example.api.shared.openapi.OpenApiConfig;
import com.example.api.shared.security.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentification", description = "Inscription, connexion et informations du compte courant.")
public class AuthController {

    private final AuthUseCase useCase;

    public AuthController(AuthUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/register")
    @Operation(summary = "Créer un compte", description = "Inscription d'un nouvel utilisateur.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte créé"),
            @ApiResponse(responseCode = "400", description = "Données invalides")
    })
    public ResponseEntity<Void> register(
            @Valid @RequestBody RegisterRequestDto dto) {

        useCase.register(dto);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    @Operation(summary = "Se connecter", description = "Retourne un token JWT à utiliser dans Authorization: Bearer <token>.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connexion réussie (JWT retourné)"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "401", description = "Identifiants invalides")
    })
    public ResponseEntity<LoginResponseDto> login(
            @Valid @RequestBody LoginRequestDto dto) {

        LoginResponseDto response = useCase.login(dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    @Operation(summary = "Récupérer mon profil (me)", description = "Retourne les informations de l'utilisateur authentifié (extraites du JWT/serveur).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Informations retournées"),
            @ApiResponse(responseCode = "401", description = "Authentification requise")
    })
    public ResponseEntity<MeResponseDto> me(@AuthenticationPrincipal JwtPrincipal principal) {
        MeResponseDto me = useCase.getMe(principal);
        return ResponseEntity.ok(me);
    }
}