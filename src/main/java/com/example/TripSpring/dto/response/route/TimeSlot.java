package com.example.TripSpring.dto.response.route;

import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlot {
    private LocalTime startTime;
    private LocalTime endTime;
    private double crowdLevel;  // 0.0 ~ 1.0
}