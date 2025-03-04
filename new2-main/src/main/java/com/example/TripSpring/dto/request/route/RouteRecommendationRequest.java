package com.example.TripSpring.dto.request.route;

import lombok.Data;
import java.util.List;
import java.time.LocalDateTime;

@Data
public class RouteRecommendationRequest {
    private List<OptimizedSchedule> optimizedSchedules;

    @Data
    public static class OptimizedSchedule {
        private String name;
        private Location location;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String type;
        private int priority;
    }

    @Data
    public static class Location {
        private double latitude;
        private double longitude;
        private String name;

        // 기본 생성자 추가
        public Location() {}

        // 문자열을 파싱하는 생성자 추가
        public Location(String locationString) {
            this.name = locationString;
        }
    }
}