package com.example.TripSpring.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScheduleProcessResponse {
    private String location;
    private String time;
    private int priority;
    private String type;
}