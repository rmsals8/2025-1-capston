package com.example.TripSpring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteCalculatorService {
    
    private final TmapService tmapService;
    
    /**
     * 두 지점 간의 거리와 이동 시간을 계산
     */
    public RouteInfo calculateRoute(double startLat, double startLon, double endLat, double endLon, String mode) {
        try {
            // TmapService를 통해 경로 정보 조회
            Map<String, Object> routeData = tmapService.getDetailedRoute(startLat, startLon, endLat, endLon, mode);
            
            // 필요한 정보 추출
            double distance = extractDistance(routeData);
            int duration = extractDuration(routeData);
            double trafficRate = extractTrafficRate(routeData);
            String transportMode = determineTransportMode(mode, distance);
            
            return RouteInfo.builder()
                    .distance(distance)
                    .duration(duration)
                    .trafficRate(trafficRate)
                    .transportMode(transportMode)
                    .build();
        } catch (Exception e) {
            log.error("Failed to calculate route", e);
            // 실패 시 기본값으로 대체
            return createFallbackRouteInfo(startLat, startLon, endLat, endLon, mode);
        }
    }
    
    private double extractDistance(Map<String, Object> routeData) {
        try {
            // 실제 구현에서는 API 응답에서 거리 추출
            double meters = (double) routeData.getOrDefault("totalDistance", 0.0);
            return meters / 1000.0; // 미터를 킬로미터로 변환
        } catch (Exception e) {
            log.warn("Failed to extract distance", e);
            return 0.0;
        }
    }
    
    private int extractDuration(Map<String, Object> routeData) {
        try {
            // 실제 구현에서는 API 응답에서 시간 추출
            return (int) routeData.getOrDefault("totalTime", 0);
        } catch (Exception e) {
            log.warn("Failed to extract duration", e);
            return 0;
        }
    }
    
    private double extractTrafficRate(Map<String, Object> routeData) {
        try {
            // 실제 구현에서는 API 응답에서 교통 정보 추출
            if (routeData.containsKey("trafficInfo")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> trafficInfo = (Map<String, Object>) routeData.get("trafficInfo");
                return (double) trafficInfo.getOrDefault("congestion", 0.5);
            }
            return 0.5; // 기본값
        } catch (Exception e) {
            log.warn("Failed to extract traffic rate", e);
            return 0.5;
        }
    }
    
    private String determineTransportMode(String requestedMode, double distance) {
        // 요청된 모드가 없거나 자동인 경우 거리에 따라 결정
        if (requestedMode == null || "AUTO".equals(requestedMode)) {
            if (distance <= 1.0) {
                return "WALK";
            } else if (distance <= 5.0) {
                return "PUBLIC";
            } else {
                return "TAXI";
            }
        }
        return requestedMode;
    }
    
    private RouteInfo createFallbackRouteInfo(double startLat, double startLon, double endLat, double endLon, String mode) {
        // 직선 거리 계산
        double distance = calculateDistance(startLat, startLon, endLat, endLon);
        
        // 모드에 따른 속도 가정
        double speedKmh;
        String transportMode = determineTransportMode(mode, distance);
        
        switch (transportMode) {
            case "WALK":
                speedKmh = 4.0; // 도보 시속 4km
                break;
            case "PUBLIC":
                speedKmh = 15.0; // 대중교통 평균 시속 15km
                break;
            case "TAXI":
                speedKmh = 30.0; // 택시 평균 시속 30km
                break;
            default:
                speedKmh = 20.0; // 기본값
        }
        
        // 이동 시간 계산 (분 단위)
        int duration = (int) Math.ceil(distance / speedKmh * 60);
        
        return RouteInfo.builder()
                .distance(distance)
                .duration(duration)
                .trafficRate(0.5) // 기본값
                .transportMode(transportMode)
                .build();
    }
    
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // 하버사인 공식을 사용한 거리 계산
        double earthRadius = 6371; // 지구 반경(km)
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return earthRadius * c; // 킬로미터 단위 거리
    }
    
    @Data
    @Builder
    public static class RouteInfo {
        private double distance; // 킬로미터
        private int duration; // 분
        private double trafficRate; // 교통 혼잡도 (0-1)
        private String transportMode; // WALK, PUBLIC, TAXI
    }
}
