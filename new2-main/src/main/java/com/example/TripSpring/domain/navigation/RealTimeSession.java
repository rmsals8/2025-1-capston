//src/main/java/com/example/TripSpring/domain/navigation/RealTimeSession.java
package com.example.TripSpring.domain.navigation;

import com.example.TripSpring.dto.domain.Location;
import com.example.TripSpring.dto.route.RouteDetails;
import com.example.TripSpring.dto.domain.route.TransportMode;
import com.example.TripSpring.dto.navigation.NavigationResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Slf4j
public class RealTimeSession {
    private String navigationId;
    private Location currentLocation;
    private Location destination;
    private Location nextWaypoint;
    private List<Location> remainingWaypoints;
    private NavigationResponse.NavigationStatus status;
    private TransportMode currentTransportMode;
    private LocalDateTime lastUpdate;
    private RouteDetails currentRoute;
    private int remainingDistance;  // meters
    private int remainingTime;      // seconds
    private double currentCongestion;
    private String currentInstruction;
    private List<String> upcomingInstructions;

    public RealTimeSession(String navigationId, Location start, Location destination) {
        this.navigationId = navigationId;
        this.currentLocation = start;
        this.destination = destination;
        this.status = NavigationResponse.NavigationStatus.ACTIVE;
        this.lastUpdate = LocalDateTime.now();
        this.remainingWaypoints = new ArrayList<>();
        this.upcomingInstructions = new ArrayList<>();
    }

    public void updateLocation(Location newLocation) {
        this.currentLocation = newLocation;
        this.lastUpdate = LocalDateTime.now();
        updateProgress();
    }

    public void updateRoute(RouteDetails newRoute) {
        this.currentRoute = newRoute;
        updateProgress();
        generateInstructions();
    }

    private void updateProgress() {
        if (currentRoute != null) {
            // 남은 거리 계산
            this.remainingDistance = calculateRemainingDistance();
            
            // 남은 시간 계산
            this.remainingTime = calculateRemainingTime();
            
            // 다음 경유지 업데이트
            updateNextWaypoint();
        }
    }

    private int calculateRemainingDistance() {
        if (currentLocation == null || destination == null) {
            return 0;
        }

        // 현재 위치에서 목적지까지의 직선 거리 계산 (미터 단위)
        return (int) calculateDistance(currentLocation, destination);
    }

    private int calculateRemainingTime() {
        if (remainingDistance == 0) {
            return 0;
        }

        // 현재 이동수단의 평균 속도를 기반으로 예상 시간 계산
        double averageSpeed = getAverageSpeed(currentTransportMode);
        return (int) (remainingDistance / averageSpeed);
    }

    private void updateNextWaypoint() {
        if (!remainingWaypoints.isEmpty()) {
            this.nextWaypoint = remainingWaypoints.get(0);
        } else {
            this.nextWaypoint = destination;
        }
    }

    private void generateInstructions() {
        upcomingInstructions.clear();
        if (currentRoute != null && currentRoute.getSegments() != null) {
            currentRoute.getSegments().forEach(segment -> {
                String instruction = String.format("%s에서 %s 방향으로 %s", 
                    segment.getStartLocationName(),
                    segment.getEndLocationName(),
                    getDirectionInstruction(segment.getMode()));
                upcomingInstructions.add(instruction);
            });
        }
    }

    private double calculateDistance(Location loc1, Location loc2) {
        final int R = 6371000; // 지구의 반지름 (미터)
        double lat1 = Math.toRadians(loc1.getLatitude());
        double lat2 = Math.toRadians(loc2.getLatitude());
        double lon1 = Math.toRadians(loc1.getLongitude());
        double lon2 = Math.toRadians(loc2.getLongitude());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return R * c;
    }

    private double getAverageSpeed(TransportMode mode) {
        // 평균 속도 (미터/초)
        return switch (mode) {
            case WALK -> 1.4;     // 5 km/h
            case BUS -> 8.3;      // 30 km/h
            case SUBWAY -> 11.1;  // 40 km/h
            case TAXI -> 11.1;    // 40 km/h
        };
    }

    private String getDirectionInstruction(TransportMode mode) {
        return switch (mode) {
            case WALK -> "도보로 이동";
            case BUS -> "버스로 이동";
            case SUBWAY -> "지하철로 이동";
            case TAXI -> "택시로 이동";
        };
    }

    public Location getExpectedLocation() {
        if (currentRoute == null || currentRoute.getSegments().isEmpty()) {
            return currentLocation;
        }

        // 현재 진행 중인 세그먼트의 예상 위치 반환
        var currentSegment = currentRoute.getSegments().get(0);
        return new Location(
            currentSegment.getEndLocation().getLatitude(),
            currentSegment.getEndLocation().getLongitude()
        );
    }
    public void updateRoute(Map<String, Object> routeInfo) {
        try {
            // Map에서 필요한 정보 추출
            Map<String, Object> properties = (Map<String, Object>) 
                ((List<Map<String, Object>>) routeInfo.get("features")).get(0).get("properties");
            
            this.remainingDistance = ((Number) properties.getOrDefault("totalDistance", 0)).intValue();
            this.remainingTime = ((Number) properties.getOrDefault("totalTime", 0)).intValue();
            
            // 안내 메시지 업데이트
            updateInstructions(routeInfo);
        } catch (Exception e) {
            log.error("Error updating route: {}", e.getMessage());
        }
    }

    private void updateInstructions(Map<String, Object> routeInfo) {
        List<String> instructions = new ArrayList<>();
        List<Map<String, Object>> features = (List<Map<String, Object>>) routeInfo.get("features");
        
        for (Map<String, Object> feature : features) {
            Map<String, Object> properties = (Map<String, Object>) feature.get("properties");
            if (properties != null && properties.containsKey("description")) {
                instructions.add((String) properties.get("description"));
            }
        }
        
        this.upcomingInstructions = instructions;
    }
}