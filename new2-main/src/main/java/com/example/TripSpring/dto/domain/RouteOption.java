package com.example.TripSpring.dto.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteOption {
    private List<RouteSegment> segments;
    private double totalDistance;
    private int totalDuration;
    private double totalCost;
    private String summary;
    private List<String> alerts;
}