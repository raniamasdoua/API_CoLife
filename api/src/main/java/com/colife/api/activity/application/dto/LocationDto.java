package com.colife.api.activity.application.dto;

public record LocationDto(
        String room,
        String street,
        String complement,
        String postalCode,
        String city
) {
}
