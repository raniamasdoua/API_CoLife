package com.example.api.user.application.dto;

import com.example.api.user.domain.Role;

public record UserResponseDto(
        Long id,
        String email,
        Role role
) {}
