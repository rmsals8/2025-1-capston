package com.example.TripSpring.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.example.TripSpring.dto.domain.Location;
import com.example.TripSpring.dto.domain.TrafficInfo;
import org.springframework.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FirstMapService {
    @Value("${app.api.tmap}")
    private String apiKey;
    private final RestTemplate restTemplate;

    @Autowired
    public FirstMapService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public TrafficInfo getTrafficInfo(Location start, Location end) {
        try {
            // T Map API 호출 시도
            String url = String.format("https://apis.openapi.sk.com/tmap/routes?startX=%f&startY=%f&endX=%f&endY=%f&appKey=%s",
                start.getLongitude(), start.getLatitude(), 
                end.getLongitude(), end.getLatitude(), apiKey);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getBody() != null) {
                // API 응답 처리
                return extractTrafficInfo(response.getBody());
            }
        } catch (Exception e) {
            log.error("Failed to get traffic info: {}", e.getMessage());
        }

        // API 호출 실패시 예상 값 반환
        return estimateTrafficInfo(start, end);
    }
    @SuppressWarnings("unchecked")
    private TrafficInfo extractTrafficInfo(Map<String, Object> responseBody) {
        try {
            Map<String, Object> features = (Map<String, Object>) ((List<?>) responseBody.get("features")).get(0);
            Map<String, Object> properties = (Map<String, Object>) features.get("properties");
            
            double totalTime = ((Number) properties.get("totalTime")).doubleValue();
            double totalDistance = ((Number) properties.get("totalDistance")).doubleValue() / 1000.0; // meters to km
            
            return new TrafficInfo(1.0, (int) totalTime, totalDistance);
        } catch (Exception e) {
            log.error("Error extracting traffic info: {}", e.getMessage());
            return estimateTrafficInfo(null, null);
        }
    }

    private TrafficInfo estimateTrafficInfo(Location start, Location end) {
        if (start == null || end == null) {
            return new TrafficInfo(1.0, 15, 1.0); // 기본값
        }

        // 간단한 거리 계산 (Haversine formula)
        double distance = calculateDistance(start, end);
        int estimatedTime = (int) (distance * 3); // 평균 속도 20km/h 가정
        
        return new TrafficInfo(1.0, estimatedTime, distance);
    }

    private double calculateDistance(Location start, Location end) {
        final int R = 6371; // Earth's radius in kilometers

        double lat1 = Math.toRadians(start.getLatitude());
        double lon1 = Math.toRadians(start.getLongitude());
        double lat2 = Math.toRadians(end.getLatitude());
        double lon2 = Math.toRadians(end.getLongitude());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return R * c;
    }
}