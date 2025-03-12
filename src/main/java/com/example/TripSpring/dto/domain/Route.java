package com.example.TripSpring.dto.domain;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
public class Route {
    private List<Schedule> schedules;
    private double totalTime;
    private double totalDistance;
    private double totalCost;
}