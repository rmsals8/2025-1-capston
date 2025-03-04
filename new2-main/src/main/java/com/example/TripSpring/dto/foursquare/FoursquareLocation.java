package com.example.TripSpring.dto.foursquare;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoursquareLocation {
    private String address;
    private String locality;
    private String region;
    private String country;
    private double lat;
    private double lng;
}