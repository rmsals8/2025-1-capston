package com.example.TripSpring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.*;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceAnalyzerService {
    
    private final RestTemplate restTemplate;

    private String apiKey = "fsq3VpVQLn5hZptfpIHLogZHRb7vAbteiSkiUlZT4QvpC8U=";
    
    private static final String BASE_URL = "https://api.foursquare.com/v3";

    /**
     * 장소 상세 분석 결과를 담는 클래스
     */
    public record PlaceAnalysis(
        String name,
        boolean isOpen,
        LocalTime openTime,
        LocalTime closeTime,
        List<String> peakHours,
        double rating,
        int totalVisits,
        double popularity,
        String status
    ) {}

    /**
     * 장소 정보를 분석하여 상세 결과를 반환
     */
    public PlaceAnalysis analyzePlaceDetails(String placeName, double lat, double lng) {
        try {
            // 1. 장소 검색으로 FSQ_ID 획득
            String fsqId = searchPlace(placeName, lat, lng);
            if (fsqId == null) {
                log.warn("Place not found: {}", placeName);
                return null;
            }

            // 2. 장소 상세 정보 조회
            Map<String, Object> placeDetails = getPlaceDetails(fsqId);
            if (placeDetails == null) {
                log.warn("Failed to get place details for fsqId: {}", fsqId);
                return null;
            }

            // 3. 데이터 파싱 및 분석
            return parseAndAnalyzeDetails(placeDetails);

        } catch (Exception e) {
            log.error("Error analyzing place details for: {}", placeName, e);
            return null;
        }
    }

    /**
     * 장소 검색하여 FSQ_ID 반환
     */
    private String searchPlace(String name, double lat, double lng) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/places/search")
                .queryParam("query", name)
                .queryParam("ll", lat + "," + lng)
                .queryParam("radius", 1000)
                .queryParam("limit", 1)
                .build()
                .toString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", apiKey);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );

            if (response.getBody() != null) {
                @SuppressWarnings("null")
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("results");
                if (!results.isEmpty()) {
                    return (String) results.get(0).get("fsq_id");
                }
            }
            return null;

        } catch (Exception e) {
            log.error("Error searching place: {}", name, e);
            return null;
        }
    }

    /**
     * FSQ_ID로 장소 상세 정보 조회
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getPlaceDetails(String fsqId) {
        try {
            String url = BASE_URL + "/places/" + fsqId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", apiKey);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("Error getting place details for fsqId: {}", fsqId, e);
            return null;
        }
    }

    /**
     * 장소 상세 정보 파싱 및 분석
     */
    @SuppressWarnings("unchecked")
    private PlaceAnalysis parseAndAnalyzeDetails(Map<String, Object> details) {
        try {
            String name = (String) details.get("name");
            
            // 영업시간 정보 파싱
            Map<String, Object> hours = (Map<String, Object>) details.get("hours");
            boolean isOpen = hours != null && "Open".equals(hours.get("status"));
            
            LocalTime openTime = LocalTime.of(9, 0);  // 기본값
            LocalTime closeTime = LocalTime.of(22, 0); // 기본값
            
            if (hours != null && hours.get("regular") != null) {
                List<Map<String, Object>> regularHours = (List<Map<String, Object>>) hours.get("regular");
                if (!regularHours.isEmpty()) {
                    openTime = parseTime((String) regularHours.get(0).get("open"));
                    closeTime = parseTime((String) regularHours.get(0).get("close"));
                }
            }

            // 피크타임 정보 파싱
            List<String> peakHours = new ArrayList<>();
            if (hours != null && hours.get("popular") != null) {
                List<Map<String, Object>> popularHours = (List<Map<String, Object>>) hours.get("popular");
                for (Map<String, Object> popularHour : popularHours) {
                    List<Integer> peaks = (List<Integer>) popularHour.get("peak_hours");
                    if (peaks != null) {
                        for (Integer hour : peaks) {
                            peakHours.add(String.format("%02d:00", hour));
                        }
                    }
                }
            }

            // 평점 및 방문자 수 파싱
            Map<String, Object> rating = (Map<String, Object>) details.get("rating");
            double ratingValue = rating != null ? ((Number) rating.get("rating")).doubleValue() : 0.0;
            
            Map<String, Object> stats = (Map<String, Object>) details.get("stats");
            int totalVisits = stats != null ? ((Number) stats.get("total_visits")).intValue() : 0;

            // 인기도 계산 (평점과 방문자 수 기반)
            double popularity = calculatePopularity(ratingValue, totalVisits);

            // 현재 상태 결정
            String status = determineCurrentStatus(isOpen, LocalTime.now(), openTime, closeTime, peakHours);

            return new PlaceAnalysis(
                name,
                isOpen,
                openTime,
                closeTime,
                peakHours,
                ratingValue,
                totalVisits,
                popularity,
                status
            );

        } catch (Exception e) {
            log.error("Error parsing place details", e);
            return null;
        }
    }

    /**
     * "HH:mm" 형식의 시간 문자열을 LocalTime으로 파싱
     */
    private LocalTime parseTime(String timeStr) {
        try {
            int hours = Integer.parseInt(timeStr.substring(0, 2));
            int minutes = Integer.parseInt(timeStr.substring(2));
            return LocalTime.of(hours, minutes);
        } catch (Exception e) {
            return LocalTime.of(9, 0); // 기본값
        }
    }

    /**
     * 평점과 방문자 수를 기반으로 인기도 계산 (0.0 ~ 1.0)
     */
    private double calculatePopularity(double rating, int totalVisits) {
        // 평점 정규화 (0-10 → 0-1)
        double normalizedRating = rating / 10.0;
        
        // 방문자 수 정규화 (로그 스케일 사용, 최대 10000명 기준)
        double normalizedVisits = Math.min(Math.log10(totalVisits + 1) / 4.0, 1.0);
        
        // 가중 평균 계산 (평점 60%, 방문자 수 40%)
        return (normalizedRating * 0.6) + (normalizedVisits * 0.4);
    }

    /**
     * 현재 시간 기준으로 장소의 상태 결정
     */
    private String determineCurrentStatus(boolean isOpen, LocalTime currentTime, 
                                        LocalTime openTime, LocalTime closeTime, 
                                        List<String> peakHours) {
        if (!isOpen) {
            return "CLOSED";
        }

        // 피크타임 체크
        boolean isPeakHour = peakHours.stream()
            .map(this::parseTime)
            .anyMatch(time -> time.equals(currentTime.withMinute(0)));

        if (isPeakHour) {
            return "PEAK_HOURS";
        }

        // 영업 시작/종료 임박 체크 (1시간 기준)
        if (currentTime.isBefore(openTime.plusHours(1))) {
            return "JUST_OPENED";
        }
        if (currentTime.isAfter(closeTime.minusHours(1))) {
            return "CLOSING_SOON";
        }

        return "NORMAL";
    }
}