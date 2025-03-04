package com.example.TripSpring.service;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteAnalyzerService {

    private final RestTemplate restTemplate;


    @Value("${app.api.tmap}")
    private String apiKey;

    private static final String BASE_URL = "https://apis.openapi.sk.com/tmap";

    @Getter
    @Builder
    public static class RouteSegment {
        private final String roadName;
        private final double distance;
        private final int duration;
        private final double congestion;
        private final String guidance;
    }

    @Getter
    @Builder
    public static class RouteAnalysis {
        private final double distance;           // km
        private final int duration;             // minutes
        private final double congestionLevel;    // 0.0 ~ 1.0
        private final String status;
        private final List<RouteSegment> segments;
        private final LocalDateTime estimatedArrival;
            // congestionLevel과 segments에 대한 명시적 getter 추가
        public double getCongestionLevel() {
            return this.congestionLevel;
        }

        public List<RouteSegment> getSegments() {
            return this.segments;
        }
    }

    public RouteAnalysis analyzeRoute(
        double startLat,
        double startLon,
        double endLat,
        double endLon,
        LocalDateTime departureTime
    ) {
        try {
            // T-map API 호출
            String routeResponse = getRouteInfo(startLat, startLon, endLat, endLon);
            String trafficResponse = getTrafficInfo(startLat, startLon);

            // 응답 파싱 및 분석
            double distance = extractDistance(routeResponse);
            int duration = extractDuration(routeResponse);
            double congestion = extractCongestion(trafficResponse);
            List<RouteSegment> segments = extractSegments(routeResponse);

            // 예상 도착 시간 계산 (교통 상황 반영)
            LocalDateTime estimatedArrival = calculateEstimatedArrival(
                departureTime,
                duration,
                congestion
            );

            return RouteAnalysis.builder()
                .distance(distance)
                .duration(duration)
                .congestionLevel(congestion)
                .status(determineTrafficStatus(congestion))
                .segments(segments)
                .estimatedArrival(estimatedArrival)
                .build();

        } catch (Exception e) {
            log.error("Error analyzing route", e);
            return createDefaultRouteAnalysis(departureTime);
        }
    }

    private String getRouteInfo(double startLat, double startLon, double endLat, double endLon) {
        try {
            // 1. URL 설정
            String url = BASE_URL + "/routes/pedestrian";
    
            // 2. 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("appKey", apiKey);
            
            // 3. 요청 바디 설정
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("startX", String.format("%.7f", startLon));
            requestBody.put("startY", String.format("%.7f", startLat));
            requestBody.put("endX", String.format("%.7f", endLon));
            requestBody.put("endY", String.format("%.7f", endLat));
            requestBody.put("reqCoordType", "WGS84GEO");
            requestBody.put("resCoordType", "WGS84GEO");
            requestBody.put("startName", URLEncoder.encode("출발지", StandardCharsets.UTF_8));
            requestBody.put("endName", URLEncoder.encode("도착지", StandardCharsets.UTF_8));
            requestBody.put("searchOption", "0");
    
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            log.debug("T-map Route API Request URL: {}", url);
            log.debug("T-map Route API Headers: {}", headers);
            log.debug("T-map Route API Request Body: {}", requestBody);
    
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
            );
    
            log.debug("T-map Route API Response: {}", response.getBody());
            return response.getBody();
    
        } catch (Exception e) {
            log.error("Error getting route info: {}", e.getMessage());
            return "";
        }
    }

private String getTrafficInfo(double lat, double lon) {
    try {
        // 1. URL 구성
        String url = String.format("%s/traffic", BASE_URL);
        
        // 2. API 요청 파라미터 설정
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
            .queryParam("version", "1")
            .queryParam("appKey", apiKey)  // API 키를 쿼리 파라미터로 추가
            .queryParam("lat", String.format("%.7f", lat))
            .queryParam("lon", String.format("%.7f", lon))
            .queryParam("radius", "2")
            .queryParam("reqCoordType", "WGS84GEO")
            .queryParam("resCoordType", "WGS84GEO")
            .queryParam("searchType", "all");

        // 3. 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // 4. API 호출
        HttpEntity<?> entity = new HttpEntity<>(headers);
        
        log.debug("T-map Traffic API Request URL: {}", builder.toUriString());
        log.debug("T-map Traffic API Headers: {}", headers);

        ResponseEntity<String> response = restTemplate.exchange(
            builder.toUriString(),
            HttpMethod.GET,
            entity,
            String.class
        );

        log.debug("T-map Traffic API Response: {}", response.getBody());
        return response.getBody();

    } catch (Exception e) {
        log.error("Error getting traffic info: {}", e.getMessage());
        return "";
    }
}

    private double extractDistance(String routeResponse) {
        // 실제 구현에서는 JSON 파싱 후 거리 추출
        // 현재는 예시 값 반환
        return 5.0;  // 5km
    }

    private int extractDuration(String routeResponse) {
        // 실제 구현에서는 JSON 파싱 후 소요 시간 추출
        // 현재는 예시 값 반환
        return 30;  // 30분
    }

    private double extractCongestion(String trafficResponse) {
        // 실제 구현에서는 JSON 파싱 후 혼잡도 추출
        // 현재는 예시 값 반환
        return 0.5;  // 50% 혼잡
    }

    private List<RouteSegment> extractSegments(String routeResponse) {
        // 실제 구현에서는 JSON 파싱 후 세그먼트 정보 추출
        // 현재는 예시 데이터 반환
        return Collections.singletonList(
            RouteSegment.builder()
                .roadName("테헤란로")
                .distance(1.2)
                .duration(15)
                .congestion(0.5)
                .guidance("직진")
                .build()
        );
    }

    private LocalDateTime calculateEstimatedArrival(
        LocalDateTime departureTime,
        int baseDuration,
        double congestion
    ) {
        // 혼잡도에 따른 추가 시간 계산 (최대 50% 추가)
        int additionalMinutes = (int) (baseDuration * (congestion * 0.5));
        return departureTime.plusMinutes(baseDuration + additionalMinutes);
    }

    private RouteAnalysis createDefaultRouteAnalysis(LocalDateTime departureTime) {
        return RouteAnalysis.builder()
            .distance(0.0)
            .duration(30)
            .congestionLevel(0.5)
            .status("정보 없음")
            .segments(new ArrayList<>())
            .estimatedArrival(departureTime.plusMinutes(30))
            .build();
    }

    private String determineTrafficStatus(double congestion) {
        if (congestion < 0.3) return "원활";
        if (congestion < 0.7) return "보통";
        return "혼잡";
    }
}