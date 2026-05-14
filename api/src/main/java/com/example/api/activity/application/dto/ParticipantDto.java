package com.example.api.activity.application.dto;

public record ParticipantDto(
        Long id,
        String firstName,
        String lastName,
        String email
) {}
