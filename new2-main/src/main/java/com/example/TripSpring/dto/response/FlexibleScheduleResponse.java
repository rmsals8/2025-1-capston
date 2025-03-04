package com.example.TripSpring.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlexibleScheduleResponse {
    private List<RouteOption> routeOptions;
    private OptimizationMetrics metrics;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RouteOption {
        private String id;
        private List<ScheduleItem> schedules;
        private double totalDistance;
        private int totalDuration;
        private double totalCost;
        private List<RouteSegment> segments;
        private Map<String, Double> scores; // 각 최적화 점수 요소
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScheduleItem {
        private String id;
        private String name;
        private String location;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String type; // FIXED 또는 FLEXIBLE
        private double latitude;
        private double longitude;
        private int duration;
        private boolean isOptimized;
        private String placeType; // 장소 유형 (마트, 서점 등)
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RouteSegment {
        private String from;
        private String to;
        private double distance;
        private int duration;
        private double trafficRate;
        private String transportMode;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OptimizationMetrics {
        private int processedCombinations;
        private int filteredOptions;
        private long processingTimeMs;
        private String algorithm;
    }
}
