package com.example.api.user.presentation;

import com.example.api.user.application.UserUseCase;
import com.example.api.user.application.dto.UserResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserUseCase userUseCase;

    public UserController(UserUseCase userUseCase) {
        this.userUseCase = userUseCase;
    }

    /**
     * Un COLLABORATOR ne peut accéder qu'à son propre profil (id = son userId).
     * Un ADMIN peut accéder à n'importe quel profil.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.userId")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        UserResponseDto user = userUseCase.getUserById(id);
        return ResponseEntity.ok(user);
    }
}
