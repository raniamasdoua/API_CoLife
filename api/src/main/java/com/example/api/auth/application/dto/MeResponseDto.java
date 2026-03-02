package com.example.api.auth.application.dto;

import com.example.api.user.domain.Role;

public record MeResponseDto(
        Long id,
        String email,
        Role role
) {}
