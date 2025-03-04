package com.example.TripSpring.dto.response;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class RouteMetrics {
    private double totalDistance;
    private int totalTravelTime;
    private double totalCost;
    private double averageTrafficRate;
}