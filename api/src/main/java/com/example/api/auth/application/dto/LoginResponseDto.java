package com.example.api.auth.application.dto;

public record LoginResponseDto(
        String accessToken,
        String type
) {
    public static LoginResponseDto of(String accessToken) {
        return new LoginResponseDto(accessToken, "Bearer");
    }
}
