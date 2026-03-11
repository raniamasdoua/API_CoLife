package com.example.api.user.presentation;

import com.example.api.shared.security.JwtPrincipal;
import com.example.api.user.application.UserUseCase;
import com.example.api.user.application.dto.UpdateProfileRequestDto;
import com.example.api.user.application.dto.UserResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserUseCase userUseCase;

    public UserController(UserUseCase userUseCase) {
        this.userUseCase = userUseCase;
    }

    /**
     * GET /user/{id}
     * Un COLLABORATOR ne peut accéder qu'à son propre profil.
     * Un ADMIN peut accéder à n'importe quel profil.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.userId")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userUseCase.getUserById(id));
    }

    /**
     * PATCH /user/{id}/profile
     * Modifie uniquement bio, phone et address.
     * Un COLLABORATOR ne peut modifier que son propre profil.
     * Un ADMIN peut modifier n'importe quel profil.
     */
    @PatchMapping("/{id}/profile")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.userId")
    public ResponseEntity<UserResponseDto> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProfileRequestDto request,
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(userUseCase.updateProfile(id, request));
    }
}
