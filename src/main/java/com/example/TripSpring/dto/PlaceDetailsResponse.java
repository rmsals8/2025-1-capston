package com.example.TripSpring.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PlaceDetailsResponse {
    private PlaceDetails result;
    private String status;
}