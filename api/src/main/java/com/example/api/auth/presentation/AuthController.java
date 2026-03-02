package com.example.api.auth.presentation;

import com.example.api.auth.application.AuthUseCase;
import com.example.api.auth.application.dto.LoginRequestDto;
import com.example.api.auth.application.dto.LoginResponseDto;
import com.example.api.auth.application.dto.MeResponseDto;
import com.example.api.auth.application.dto.RegisterRequestDto;
import com.example.api.shared.security.JwtPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
            @Valid @RequestBody LoginRequestDto dto) {

        LoginResponseDto response = useCase.login(dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponseDto> me(@AuthenticationPrincipal JwtPrincipal principal) {
        MeResponseDto me = useCase.getMe(principal);
        return ResponseEntity.ok(me);
    }
}