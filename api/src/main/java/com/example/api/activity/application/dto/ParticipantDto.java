package com.example.api.activity.application.dto;

import java.util.UUID;

public record ParticipantDto(
        UUID id,
        String firstName,
        String lastName,
        String email
) {}
