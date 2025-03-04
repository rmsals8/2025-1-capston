package com.example.TripSpring.dto;

import com.example.TripSpring.dto.analysis.ScheduleAnalysis;
import com.example.TripSpring.dto.domain.Route;
import lombok.Data;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
public class RouteOptimizationResult {
    private Route route;
    private ScheduleAnalysis analysis;
    private List<String> optimizationDetails;
}