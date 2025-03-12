package com.example.TripSpring.dto.response.route;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedPlaceDetails {
    private String description;
    private String category;
    private OperatingHours operatingHours;
    private ContactInfo contactInfo;
    private double rating;
    private int reviewCount;
    private List<String> tags;
    private Map<String, String> amenities;
    private PeakHours peakHours;
}