package com.example.TripSpring.dto.response;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TripSegment {
    private String fromLocation;
    private String toLocation;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private double distance;
    private int estimatedTime;
    private double trafficRate;
    private String reason;
}
