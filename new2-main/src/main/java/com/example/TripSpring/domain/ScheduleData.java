package com.example.TripSpring.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScheduleData {
    private String location;
    private String time;
    private int priority;
    private ScheduleType type;
}