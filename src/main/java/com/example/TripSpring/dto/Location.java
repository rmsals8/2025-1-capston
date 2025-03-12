package com.example.TripSpring.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Location {
    private String navigationId;
    private Double latitude;
    private Double longitude;
    private Long timestamp;
    private Double accuracy;
}