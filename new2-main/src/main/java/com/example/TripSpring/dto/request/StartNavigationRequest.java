package com.example.TripSpring.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

import com.example.TripSpring.dto.domain.RoutePreferences;
import com.example.TripSpring.dto.domain.route.TransportMode;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartNavigationRequest {
    @NotNull(message = "현재 위치는 필수입니다")
    private Location currentLocation;
    
    // @NotEmpty(message = "선택된 경로는 필수입니다")
    // private List<RouteSelection> selectedRoutes;
    @NotNull(message = "목적지는 필수입니다")
    private Location destination;
    private RoutePreferences preferences;  // 선택적
    private NavigationOptions navigationOptions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Location {
        @NotNull(message = "Latitude is required")
        private Double latitude;
        
        @NotNull(message = "Longitude is required")
        private Double longitude;
        
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteSelection {
        private String segmentId;
        private String routeId;
        
        @NotNull(message = "Transport mode is required")
        private TransportMode  transportMode;
        
        @NotNull(message = "Start location is required")
        private Location startLocation;
        
        @NotNull(message = "End location is required")
        private Location endLocation;
        
        private Integer estimatedDuration;
        private Double distance;
        private List<Waypoint> waypoints;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NavigationPreferences {
        private boolean avoidTolls;
        private boolean avoidHighways;
        private String routeOptimization;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NavigationOptions {
        private boolean voiceGuidance;
        private boolean alternativeRoutes;
        private boolean trafficUpdates;
        private boolean realtimeRerouting;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Waypoint {
        private Double latitude;
        private Double longitude;
        private String instruction;
    }
}