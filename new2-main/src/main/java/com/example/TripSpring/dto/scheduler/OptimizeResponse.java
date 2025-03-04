package com.example.TripSpring.dto.scheduler;

import com.example.TripSpring.dto.domain.Schedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizeResponse {
    private List<Schedule> optimizedSchedules;
    private List<RouteSegment> routeSegments;
    private OptimizationMetrics metrics;
    private Map<String, List<PlaceTimeResult>> alternativeOptions; // 추가: 유연 일정별 대안 목록
    private Map<String, ScheduleAnalysis> scheduleAnalyses;
    
    /**
     * 경로 구간 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteSegment {
        private String fromLocation;
        private String toLocation;
        private double distance;
        private int estimatedTime;
        private double trafficRate;
        private String recommendedRoute;
        private Map<String, Object> realTimeTraffic;
    }
    
    /**
     * 최적화 메트릭 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptimizationMetrics {
        private double totalDistance;
        private int totalTime;
        private double totalScore;
        private double successRate; // 추가: 최적화 성공률
        private Map<String, Double> componentScores;
        private List<String> optimizationReasons;
    }
    
    /**
     * 일정 분석 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleAnalysis {
        private String locationName;
        private String bestTimeWindow;
        private double crowdLevel;
        private Map<String, Object> placeDetails;
        private List<String> optimizationFactors;
        private String visitRecommendation;
    }
    
    /**
     * 장소 및 시간 결과 (대안 표시용)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlaceTimeResult {
        private PlaceDetail place;
        private String startTime;
        private String endTime;
        private double score;
        private Map<String, Object> additionalInfo;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class PlaceDetail {
            private String id;
            private String name;
            private String formatted_address;
            private LocationDetail location;
            private double rating;
            private boolean open_now;
            private String business_status;
            private Map<String, Object> metadata;
            
            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public static class LocationDetail {
                private double lat;
                private double lng;
            }
        }
    }
    
    // 생성자 및 정적 팩토리 메서드
    public static OptimizeResponse createEmpty() {
        OptimizeResponse response = new OptimizeResponse();
        response.setOptimizedSchedules(new ArrayList<>());
        response.setRouteSegments(new ArrayList<>());
        response.setMetrics(new OptimizationMetrics());
        response.setScheduleAnalyses(new HashMap<>());
        response.setAlternativeOptions(new HashMap<>());
        return response;
    }
}