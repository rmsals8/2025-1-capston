//src/main/java/com/example/TripSpring/dto/request/OptimizeRequest.java
package com.example.TripSpring.dto.request;

import com.example.TripSpring.dto.domain.Location;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import java.time.LocalTime;
import java.util.List;

//src/main/java/com/example/TripSpring/dto/request/OptimizeRequest.java
    @Data
    @Builder
    public class OptimizeRequest {
        @NotNull(message = "경유지 목록은 필수입니다")
        private List<Location> waypoints;
        
        private OptimizeConstraints constraints;
        
        @Data
        @Builder
        public static class OptimizeConstraints {
            private Integer maxDuration;      // 최대 소요 시간 (분)
            private Double maxCost;           // 최대 비용 (원)
            private Integer maxTransfers;     // 최대 환승 횟수
            private Integer maxWalkingDistance; // 최대 도보 거리 (미터)
            private LocalTime startTime;      // 시작 시간
            private LocalTime endTime;        // 종료 시간
        }
    }