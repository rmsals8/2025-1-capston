package com.example.TripSpring.dto.response;

import java.util.List;

import com.example.TripSpring.dto.domain.Location;
import com.example.TripSpring.dto.domain.NavigationAlert;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NavigationStatus {
    private String navigationId;
    private Status status;
    private Location currentLocation;
    private Location nextWaypoint;
    private String currentInstruction;
    private List<String> upcomingInstructions;
    private int remainingDistance;
    private int remainingTime;
    private boolean rerouting;
    private List<NavigationAlert> alerts;

    public enum Status {
        STARTED, IN_PROGRESS, PAUSED, COMPLETED, REROUTING
    }
    @Data
    @Builder
    public static class NavigationAlert {
        private AlertType type;
        private String message;
        private AlertSeverity severity;

        public enum AlertType {
            TRAFFIC, ACCIDENT, REROUTE, ARRIVAL, DELAY
        }

        public enum AlertSeverity {
            INFO, WARNING, CRITICAL
        }
    }
    @Data
    @Builder
    public static class TrafficInfo {
        private double congestionLevel;  // 정체 수준 (0-1)
        private String condition;  // 교통 상황 설명
        private List<String> incidents;  // 사고/공사 등 특이사항
    }

    @Data
    @Builder
    public static class WayPoint {
        private String name;  // 경유지 이름
        private Location location;  // 위치
        private String instruction;  // 안내 메시지
        private Integer estimatedArrivalTime;  // 예상 도착 시간(분)
    }
}
