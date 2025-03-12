package com.example.TripSpring.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteRequest {
    @NotNull(message = "Start latitude is required")
    private Double startLat;
    
    @NotNull(message = "Start longitude is required")
    private Double startLon;
    
    @NotNull(message = "End latitude is required")
    private Double endLat;
    
    @NotNull(message = "End longitude is required")
    private Double endLon;
}