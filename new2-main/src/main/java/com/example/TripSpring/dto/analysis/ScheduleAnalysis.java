package com.example.TripSpring.dto.analysis;

import lombok.Data;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ScheduleAnalysis {
    private List<ScheduleDecision> decisions;
    private Map<String, List<String>> rejectedSlots;
    private double totalDistance;
    private double totalTime;
    private List<String> optimizationCriteria;

    @Data
    @AllArgsConstructor
    public static class ScheduleDecision {
        private String scheduleName;
        private LocalDateTime assignedTime;
        private String reason;
        private List<String> consideredFactors;
        private double score;
    }
}