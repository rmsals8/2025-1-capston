// src/main/java/com/example/TripSpring/dto/scheduler/FlexibleScheduleRequest.java
package com.example.TripSpring.dto.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class FlexibleScheduleRequest {
    private List<FixedScheduleDTO> fixedSchedules;
    private List<FlexibleScheduleDTO> flexibleSchedules; // flexibleOptions -> flexibleSchedules 변경

    @Data
    public static class FixedScheduleDTO {
        private String id;
        private String name;
        private String type;
        private int duration; // estimatedDuration 대신 사용
        private int priority;
        private String location; // 문자열로 위치 받기
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
        private int duration; // estimatedDuration 대신 사용
        private int priority;
    }
}