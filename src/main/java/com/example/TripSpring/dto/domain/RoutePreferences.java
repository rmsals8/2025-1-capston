package com.example.TripSpring.dto.domain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutePreferences {
    private double maxCost;
    private int maxDuration;
    private int maxTransfers;
    private boolean allowTransit;
    private boolean allowTaxi;
    private boolean allowTransfers;
    private SortCriteria sortCriteria;

    public enum SortCriteria {
        TIME,
        COST,
        DISTANCE
    }
}