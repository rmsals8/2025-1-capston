package com.example.TripSpring.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteAnalysisResponse {
    private RouteMetrics routeMetrics;
    private List<SegmentAnalysis> segments;
    private List<TimeSlotAnalysis> timeSlots;
    private WeatherAnalysis weatherInfo;
    private List<String> recommendations;
    private Map<String, Double> categoryDistribution;
    private OptimizationDetails optimization;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteMetrics {
        private double totalDistance;
        private int totalDuration;
        private String difficulty;
        private double averageSpeed;
        private int totalStops;
        private Map<String, Integer> modeSplit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SegmentAnalysis {
        private String fromLocation;
        private String toLocation;
        private double distance;
        private int duration;
        private String transportMode;
        private double crowdLevel;
        private List<String> alerts;
        private Map<String, Object> transitDetails;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSlotAnalysis {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private double crowdedness;
        private String trafficCondition;
        private boolean isRushHour;
        private boolean isOptimalTime;
        private List<String> considerations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeatherAnalysis {
        private String condition;
        private double temperature;
        private int precipitationChance;
        private String recommendation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptimizationDetails {
        private int iterationCount;
        private double originalDuration;
        private double optimizedDuration;
        private double improvementPercentage;
        private List<String> appliedStrategies;
        private Map<String, Object> scores;
    }
}