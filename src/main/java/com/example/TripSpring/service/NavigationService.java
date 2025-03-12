package com.example.TripSpring.service;


import com.example.TripSpring.dto.domain.Location;
import com.example.TripSpring.dto.domain.NavigationSession;
import com.example.TripSpring.dto.domain.Route;
import com.example.TripSpring.dto.navigation.NavigationResponse;
import com.example.TripSpring.dto.navigation.NavigationResponse.NavigationAlert;
import com.example.TripSpring.dto.request.LocationUpdate;
import com.example.TripSpring.dto.request.StartNavigationRequest;
import com.example.TripSpring.dto.response.NavigationStatus;
import com.example.TripSpring.dto.traffic.TrafficStatus;
import com.example.TripSpring.service.traffic.RealTimeTrafficService;
import com.example.TripSpring.service.TmapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class NavigationService {
    private final TmapService tmapService;
    private final RealTimeTrafficService trafficService;
    private final Map<String, NavigationSession> activeSessions = new ConcurrentHashMap<>();
    public NavigationStatus startNavigation(StartNavigationRequest request) {
        try {
            String navigationId = UUID.randomUUID().toString();
            Location currentLoc = convertRequestLocation(request.getCurrentLocation());
            Location destLoc = convertRequestLocation(request.getDestination());
            
            return startNavigation(navigationId, currentLoc, destLoc);
        } catch (Exception e) {
            log.error("Failed to start navigation: {}", e.getMessage());
            throw new RuntimeException("Failed to start navigation", e);
        }
    }

    private Location convertRequestLocation(StartNavigationRequest.Location reqLocation) {
        return new Location(
            reqLocation.getLatitude(),
            reqLocation.getLongitude(),
            reqLocation.getName()
        );
    }
    public NavigationStatus startNavigation(String navigationId, Location currentLocation, Location destination) {
        try {
            NavigationSession session = new NavigationSession(navigationId, currentLocation, destination);
            activeSessions.put(navigationId, session);

            return NavigationStatus.builder()
                .navigationId(navigationId)
                .status(NavigationStatus.Status.STARTED)
                .currentLocation(currentLocation)
                .nextWaypoint(destination)
                .remainingDistance(0)  // 초기값
                .remainingTime(0)      // 초기값
                .rerouting(false)
                .alerts(new ArrayList<>())
                .build();
        } catch (Exception e) {
            log.error("Failed to start navigation: {}", e.getMessage());
            throw new RuntimeException("Failed to start navigation", e);
        }
    }
    private Route convertMapToRoute(Map<String, Object> routeInfo) {
        Route route = new Route();
        if (routeInfo != null && routeInfo.containsKey("features")) {
            List<Map<String, Object>> features = (List<Map<String, Object>>) routeInfo.get("features");
            if (!features.isEmpty()) {
                Map<String, Object> properties = (Map<String, Object>) features.get(0).get("properties");
                route.setTotalDistance(((Number) properties.getOrDefault("totalDistance", 0)).doubleValue());
                route.setTotalTime(((Number) properties.getOrDefault("totalTime", 0)).doubleValue());
                route.setTotalCost(((Number) properties.getOrDefault("totalFare", 0)).doubleValue());
            }
        }
        return route;
    }

    private NavigationResponse createNavigationResponse(
            NavigationSession session, 
            TrafficStatus trafficStatus) {
        return NavigationResponse.builder()
            .navigationId(session.getNavigationId())
            .status(NavigationResponse.NavigationStatus.ACTIVE)
            .currentLocation(session.getCurrentLocation())
            .nextWaypoint(session.getDestination())  // 목적지를 다음 경유지로 사용
            .currentInstruction(
                String.format("목적지까지 %.1fkm 남았습니다", 
                session.getCurrentRoute().getTotalDistance() / 1000)
            )
            .upcomingInstructions(Collections.emptyList())  // 기본값
            .remainingDistance((int) session.getCurrentRoute().getTotalDistance())
            .remainingTime((int) session.getCurrentRoute().getTotalTime())
            .rerouting(false)
            .alerts(generateAlerts(session, trafficStatus))
            .build();
    }

    private NavigationResponse createErrorResponse(String navigationId, String message) {
        return NavigationResponse.builder()
            .navigationId(navigationId)
            .status(NavigationResponse.NavigationStatus.ERROR)
            .alerts(Collections.singletonList(
                NavigationAlert.builder()
                    .type(NavigationAlert.AlertType.REROUTE)
                    .message(message)
                    .severity(NavigationAlert.AlertSeverity.CRITICAL)
                    .build()
            ))
            .build();
    }

    private List<NavigationAlert> generateAlerts(
            NavigationSession session, 
            TrafficStatus trafficStatus) {
        List<NavigationAlert> alerts = new ArrayList<>();
        
        // 교통 상황 알림
        if (trafficStatus != null && trafficStatus.getCongestionLevel() > 0.7) {
            alerts.add(NavigationAlert.builder()
                .type(NavigationAlert.AlertType.TRAFFIC)
                .message("현재 구간 교통 혼잡")
                .severity(NavigationAlert.AlertSeverity.WARNING)
                .build());
        }
        
        // 도착 예정 알림
        if (session.getCurrentRoute().getTotalDistance() < 500) {  // 500m 이내
            alerts.add(NavigationAlert.builder()
                .type(NavigationAlert.AlertType.ARRIVAL)
                .message("목적지가 500m 앞에 있습니다")
                .severity(NavigationAlert.AlertSeverity.INFO)
                .build());
        }
        
        return alerts;
    }

        public NavigationStatus updateLocation(String navigationId, LocationUpdate update) {
        try {
            NavigationSession session = activeSessions.get(navigationId);
            if (session == null) {
                throw new IllegalStateException("Navigation session not found: " + navigationId);
            }

            session.updateCurrentLocation(update);
            return createNavigationStatusResponse(session);
        } catch (Exception e) {
            log.error("Failed to update location: {}", e.getMessage());
            throw new RuntimeException("Failed to update location", e);
        }
    }

    public NavigationStatus getStatus(String navigationId) {
        NavigationSession session = activeSessions.get(navigationId);
        if (session == null) {
            throw new IllegalStateException("Navigation session not found: " + navigationId);
        }
        return createNavigationStatusResponse(session);
    }

    private NavigationStatus createNavigationStatusResponse(NavigationSession session) {
        return NavigationStatus.builder()
            .navigationId(session.getNavigationId())
            .currentLocation(session.getCurrentLocation())
            .status(session.getStatus())
            .remainingDistance((int)session.getTotalDistance())
            .remainingTime(session.getTotalTime())
            .build();
    }

    public void stopNavigation(String navigationId) {
        NavigationSession session = activeSessions.remove(navigationId);
        if (session == null) {
            throw new IllegalStateException("Navigation session not found: " + navigationId);
        }
    }
}