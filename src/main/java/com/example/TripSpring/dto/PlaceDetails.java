package com.example.TripSpring.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class PlaceDetails extends Place {
    private OpeningHours opening_hours;
    private List<Review> reviews;
    private String international_phone_number;
    private String website;
    private Double price_level;
}