package com.example.TripSpring.dto.response;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class TimeSlotAnalysis {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double crowdedness;
    private String trafficCondition;
    private boolean isRushHour;
    private boolean isOptimalTime;
    private List<String> considerations;
}