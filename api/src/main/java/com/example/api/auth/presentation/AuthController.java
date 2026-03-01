package com.example.api.auth.presentation;

import com.example.api.auth.application.AuthUseCase;
import com.example.api.auth.application.dto.RegisterRequestDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthUseCase useCase;

    public AuthController(AuthUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @Valid @RequestBody RegisterRequestDto dto) {

        useCase.register(dto);

        return ResponseEntity.ok().build();
    }
}