//src/main/java/com/example/TripSpring/service/navigation/RealTimeNavigationService.java
package com.example.TripSpring.service.navigation;

import com.example.TripSpring.domain.navigation.RealTimeSession;
import com.example.TripSpring.dto.domain.Location;
import com.example.TripSpring.dto.route.RouteDetails;
import com.example.TripSpring.dto.navigation.NavigationUpdate;
import com.example.TripSpring.dto.navigation.NavigationResponse;
import com.example.TripSpring.dto.traffic.TrafficStatus;
import com.example.TripSpring.service.route.RouteDetailsService;
import com.example.TripSpring.service.traffic.RealTimeTrafficService;
import com.example.TripSpring.service.notification.NavigationNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealTimeNavigationService {
    private final SimpMessagingTemplate messagingTemplate;
    private final RouteDetailsService routeDetailsService;
    private final RealTimeTrafficService trafficService;
    private final NavigationNotificationService notificationService;
    
    private final Map<String, RealTimeSession> activeSessions = new ConcurrentHashMap<>();

    public NavigationResponse startNavigation(String navigationId, Location start, Location destination) {
        try {
            // 새 네비게이션 세션 생성
            RealTimeSession session = new RealTimeSession(navigationId, start, destination);
            
            // 초기 경로 계산
            RouteDetails initialRoute = routeDetailsService.getDetailedRoute(
                start,
                destination,
                session.getCurrentTransportMode()
            );
            session.updateRoute(initialRoute);
            
            // 세션 저장
            activeSessions.put(navigationId, session);
            
            // 초기 교통 정보 조회
            TrafficStatus trafficStatus = trafficService.getTrafficStatus(start);
            
            return createNavigationResponse(session, trafficStatus);
        } catch (Exception e) {
            log.error("Failed to start navigation: {}", e.getMessage());
            return createErrorResponse(navigationId, "Failed to start navigation");
        }
    }

    public NavigationResponse processUpdate(NavigationUpdate update) {
        RealTimeSession session = activeSessions.get(update.getNavigationId());
        if (session == null) {
            log.warn("Navigation session not found: {}", update.getNavigationId());
            return createErrorResponse(update.getNavigationId(), "Navigation session not found");
        }

        try {
            // 위치 업데이트
            session.updateLocation(update.getCurrentLocation());
            
            // 교통 정보 조회
            TrafficStatus trafficStatus = trafficService.getTrafficStatus(update.getCurrentLocation());
            
            // 경로 이탈 체크
            if (isOffRoute(session)) {
                return handleRouteDeviation(session, trafficStatus);
            }
            
            // 도착 체크
            if (hasArrived(session)) {
                return handleArrival(session);
            }

            return createNavigationResponse(session, trafficStatus);
        } catch (Exception e) {
            log.error("Error processing navigation update: {}", e.getMessage());
            return createErrorResponse(update.getNavigationId(), "Error processing update");
        }
    }

    @Scheduled(fixedRate = 10000)  // 10초마다 실행
    public void updateActiveSessions() {
        activeSessions.forEach((navigationId, session) -> {
            try {
                // 현재 위치의 교통 정보 조회
                TrafficStatus trafficStatus = trafficService.getTrafficStatus(session.getCurrentLocation());
                
                // 업데이트된 정보로 응답 생성
                NavigationResponse response = createNavigationResponse(session, trafficStatus);
                
                // WebSocket을 통해 업데이트 전송
                messagingTemplate.convertAndSend(
                    "/topic/navigation/" + navigationId,
                    response
                );
            } catch (Exception e) {
                log.error("Error updating session {}: {}", navigationId, e.getMessage());
            }
        });
    }

    private boolean isOffRoute(RealTimeSession session) {
        Location currentLocation = session.getCurrentLocation();
        Location expectedLocation = session.getExpectedLocation();
        
        // 허용 오차 범위 (미터)
        double tolerance = 50.0;
        
        return calculateDistance(currentLocation, expectedLocation) > tolerance;
    }

    private NavigationResponse handleRouteDeviation(
            RealTimeSession session,
            TrafficStatus trafficStatus) {
        session.setStatus(NavigationResponse.NavigationStatus.REROUTING);
        
        try {
            // 새 경로 계산
            RouteDetails newRoute = routeDetailsService.getDetailedRoute(
                session.getCurrentLocation(),
                session.getDestination(),
                session.getCurrentTransportMode()
            );
            
            // 세션 업데이트
            session.updateRoute(newRoute);
            
            return createNavigationResponse(session, trafficStatus);
        } catch (Exception e) {
            log.error("Failed to recalculate route: {}", e.getMessage());
            return createErrorResponse(
                session.getNavigationId(),
                "Failed to recalculate route"
            );
        }
    }

    private boolean hasArrived(RealTimeSession session) {
        return session.getRemainingDistance() < 20;  // 20미터 이내
    }

    private NavigationResponse handleArrival(RealTimeSession session) {
        session.setStatus(NavigationResponse.NavigationStatus.COMPLETED);
        
        NavigationResponse response = NavigationResponse.builder()
            .navigationId(session.getNavigationId())
            .status(NavigationResponse.NavigationStatus.COMPLETED)
            .currentLocation(session.getCurrentLocation())
            .currentInstruction("목적지에 도착했습니다")
            .remainingDistance(0)
            .remainingTime(0)
            .alerts(Collections.singletonList(
                NavigationResponse.NavigationAlert.builder()
                    .type(NavigationResponse.NavigationAlert.AlertType.ARRIVAL)
                    .message("목적지에 도착했습니다")
                    .severity(NavigationResponse.NavigationAlert.AlertSeverity.INFO)
                    .build()
            ))
            .build();
        
        // 세션 정리
        activeSessions.remove(session.getNavigationId());
        
        return response;
    }

    private NavigationResponse createNavigationResponse(
            RealTimeSession session, 
            TrafficStatus trafficStatus) {
        return NavigationResponse.builder()
            .navigationId(session.getNavigationId())
            .status(session.getStatus())
            .currentLocation(session.getCurrentLocation())
            .nextWaypoint(session.getNextWaypoint())
            .currentInstruction(session.getCurrentInstruction())
            .upcomingInstructions(session.getUpcomingInstructions())
            .remainingDistance(session.getRemainingDistance())
            .remainingTime(session.getRemainingTime())
            .rerouting(false)
            .alerts(generateAlerts(session, trafficStatus))
            .build();
    }
    private List<NavigationResponse.NavigationAlert> generateAlerts(
            RealTimeSession session, 
            TrafficStatus trafficStatus) {
        List<NavigationResponse.NavigationAlert> alerts = new ArrayList<>();
        
        // 교통 상황 알림
        if (trafficStatus != null && trafficStatus.getCongestionLevel() > 0.7) {
            alerts.add(NavigationResponse.NavigationAlert.builder()
                .type(NavigationResponse.NavigationAlert.AlertType.TRAFFIC)
                .message("현재 구간 교통 혼잡")
                .severity(NavigationResponse.NavigationAlert.AlertSeverity.WARNING)
                .build());
        }
        
        return alerts;
    }
    private NavigationResponse createErrorResponse(String navigationId, String message) {
        return NavigationResponse.builder()
            .navigationId(navigationId)
            .status(NavigationResponse.NavigationStatus.ERROR)
            .alerts(Collections.singletonList(
                NavigationResponse.NavigationAlert.builder()
                    .type(NavigationResponse.NavigationAlert.AlertType.REROUTE)
                    .message(message)
                    .severity(NavigationResponse.NavigationAlert.AlertSeverity.CRITICAL)
                    .build()
            ))
            .build();
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

    
}