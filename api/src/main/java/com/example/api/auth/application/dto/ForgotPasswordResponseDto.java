package com.example.api.auth.application.dto;

public record ForgotPasswordResponseDto(
        String message,
        String resetToken
) {}
