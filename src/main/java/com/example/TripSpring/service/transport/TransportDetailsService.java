// src/main/java/com/example/TripSpring/service/transport/TransportDetailsService.java
package com.example.TripSpring.service.transport;

import com.example.TripSpring.dto.domain.Location;
import com.example.TripSpring.dto.domain.TrafficInfo;
import com.example.TripSpring.dto.domain.route.TransportMode;
import com.example.TripSpring.dto.transport.TransportOptionDetails;
import com.example.TripSpring.service.TmapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransportDetailsService {
    private final TmapService tmapService;
    
    public List<TransportOptionDetails> getTransportOptions(Location start, Location end) {
        List<TransportOptionDetails> options = new ArrayList<>();
        
        try {
            // 도보 경로
            Map<String, Object> walkingRoute = tmapService.getDetailedRoute(
                start.getLatitude(), start.getLongitude(),
                end.getLatitude(), end.getLongitude(),
                "WALK"
            );
            options.add(createWalkingOption(walkingRoute));

            // 대중교통 경로
            String transitRoute = tmapService.getTransitRoute(
                start.getLatitude(), start.getLongitude(),
                end.getLatitude(), end.getLongitude()
            );
            options.addAll(createTransitOptions(transitRoute));

            // 택시 경로
            Map<String, Object> drivingRoute = tmapService.getDetailedRoute(
                start.getLatitude(), start.getLongitude(),
                end.getLatitude(), end.getLongitude(),
                "TAXI"
            );
            options.add(createTaxiOption(drivingRoute));

        } catch (Exception e) {
            log.error("Error getting transport options: {}", e.getMessage());
        }
        
        return options;
    }

    private TransportOptionDetails createWalkingOption(Map<String, Object> routeInfo) {
        double distance = (double) routeInfo.get("totalDistance") / 1000.0; // m -> km
        int duration = (int) routeInfo.get("totalTime");  // 분

        return TransportOptionDetails.builder()
            .mode(TransportMode.WALK)
            .distance(distance)
            .duration(duration)
            .cost(0.0)  // 도보는 비용 없음
            .congestion(0.0)  // 도보는 혼잡도 적용 안 함
            .routeDescription("도보 이동")
            .path((List) routeInfo.get("path"))
            .build();
    }

    private List<TransportOptionDetails> createTransitOptions(String transitRouteJson) {
        List<TransportOptionDetails> options = new ArrayList<>();
        // TODO: 대중교통 경로 파싱 및 변환 로직 구현
        return options;
    }

    private TransportOptionDetails createTaxiOption(Map<String, Object> routeInfo) {
        double distance = (double) routeInfo.get("totalDistance") / 1000.0;
        int duration = (int) routeInfo.get("totalTime");
        double fare = calculateTaxiFare(distance);

        return TransportOptionDetails.builder()
            .mode(TransportMode.TAXI)
            .distance(distance)
            .duration(duration)
            .cost(fare)
            .congestion((double) routeInfo.get("congestion"))
            .routeDescription("택시 이동")
            .path((List) routeInfo.get("path"))
            .build();
    }

    private double calculateTaxiFare(double distanceKm) {
        double baseFare = 3800;  // 기본요금
        double ratePerKm = 1000; // km당 요금
        return baseFare + (distanceKm * ratePerKm);
    }
}