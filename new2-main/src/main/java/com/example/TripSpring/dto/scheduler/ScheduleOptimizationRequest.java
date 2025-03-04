// src/main/java/com/example/TripSpring/dto/scheduler/ScheduleOptimizationRequest.java
package com.example.TripSpring.dto.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class ScheduleOptimizationRequest {
    private List<FixedScheduleDTO> fixedSchedules;
    private List<FlexibleScheduleDTO> flexibleSchedules;

    @Data
    public static class FixedScheduleDTO {
        private String id;
        private String name;
        private String type;
        private int duration;
        private int priority;
        private String location;
        private double latitude;
        private double longitude;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }
    
    @Data
    public static class FlexibleScheduleDTO {
        private String id;
        private String name;
        private String type;
        private int duration;
        private int priority;
    }
}