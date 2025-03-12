package com.example.TripSpring.dto.domain.route;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeoPoint {
    private double latitude;
    private double longitude;
}