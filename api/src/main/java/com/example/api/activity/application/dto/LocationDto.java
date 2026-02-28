package com.example.api.activity.application.dto;

public record LocationDto (
        String street,
        String complement,
        String postalCode,
        String city
) {
}
