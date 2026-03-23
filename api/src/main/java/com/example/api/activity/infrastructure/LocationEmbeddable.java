package com.example.api.activity.infrastructure;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class LocationEmbeddable {
    private String street;
    private String complement;
    private String postalCode;
    private String city;
}
