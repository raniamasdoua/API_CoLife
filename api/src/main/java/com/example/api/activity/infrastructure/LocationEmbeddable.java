package com.example.api.activity.infrastructure;

import com.example.api.activity.domain.LocationType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class LocationEmbeddable {
    @Enumerated(EnumType.STRING)
    private LocationType locationType;
    private String room;
    private String street;
    private String complement;
    private String postalCode;
    private String city;
}
