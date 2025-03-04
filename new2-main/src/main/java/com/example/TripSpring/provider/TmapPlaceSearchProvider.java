package com.example.TripSpring.provider;

import com.example.TripSpring.dto.domain.PlaceInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TmapPlaceSearchProvider implements PlaceSearchProvider {
    private final RestTemplate restTemplate;
    @Value("${app.api.tmap}")
    private String API_KEY;
    private final String SEARCH_URL = "https://apis.openapi.sk.com/tmap/pois";

    @Override
    public PlaceInfo searchPlace(String placeName, double lat, double lng) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("appKey", API_KEY);

            String url = UriComponentsBuilder.fromHttpUrl(SEARCH_URL)
                .queryParam("searchKeyword", placeName)
                .queryParam("centerLat", lat)
                .queryParam("centerLon", lng)
                .queryParam("count", 1)
                .build()
                .toUriString();

            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );

            if (response.getBody() != null && response.getBody().containsKey("searchPoiInfo")) {
                Map<String, Object> poiInfo = (Map<String, Object>) response.getBody().get("searchPoiInfo");
                List<Map<String, Object>> pois = (List<Map<String, Object>>) 
                    ((Map<String, Object>) poiInfo.get("pois")).get("poi");
                
                if (!pois.isEmpty()) {
                    return convertToPlaceInfo(pois.get(0));
                }
            }
            return null;
        } catch (Exception e) {
            log.error("TMap search API failed for place {}: {}", placeName, e.getMessage());
            return null;
        }
    }

    private PlaceInfo convertToPlaceInfo(Map<String, Object> poi) {
        String name = (String) poi.get("name");
        String address = (String) poi.get("upperAddrName") + " " + 
                        (String) poi.get("middleAddrName") + " " + 
                        (String) poi.get("lowerAddrName");
        
        return new PlaceInfo(
            (String) poi.get("id"),
            name,
            true, // 기본값
            inferOpeningTime(name), // 업종에 따른 추정 영업시간
            inferClosingTime(name), // 업종에 따른 추정 영업시간
            calculateCrowdLevel(poi), // 현재 시간대 기반 추정 혼잡도
            address,
            (String) poi.get("telNo"),
            (String) poi.get("firstBuildingType"),
            0.0, // 기본 평점
            inferVisitDuration(poi) // 업종별 예상 방문시간
        );
    }

    private LocalTime inferOpeningTime(String name) {
        if (name.contains("카페") || name.contains("커피")) {
            return LocalTime.of(8, 0);
        } else if (name.contains("식당") || name.contains("레스토랑")) {
            return LocalTime.of(11, 0);
        } else {
            return LocalTime.of(9, 0);
        }
    }

    private LocalTime inferClosingTime(String name) {
        if (name.contains("카페") || name.contains("커피")) {
            return LocalTime.of(22, 0);
        } else if (name.contains("식당") || name.contains("레스토랑")) {
            return LocalTime.of(21, 0);
        } else {
            return LocalTime.of(18, 0);
        }
    }

    private double calculateCrowdLevel(Map<String, Object> poi) {
        LocalTime currentTime = LocalTime.now();
        // 점심시간(11:30-13:30)과 저녁시간(18:00-20:00)에는 혼잡도 증가
        if ((currentTime.isAfter(LocalTime.of(11, 30)) && 
             currentTime.isBefore(LocalTime.of(13, 30))) ||
            (currentTime.isAfter(LocalTime.of(18, 0)) && 
             currentTime.isBefore(LocalTime.of(20, 0)))) {
            return 0.8;
        }
        return 0.4;
    }

    private LocalTime inferVisitDuration(Map<String, Object> poi) {
        String category = (String) poi.get("firstBuildingType");
        if (category.contains("식당") || category.contains("레스토랑")) {
            return LocalTime.of(1, 30); // 1시간 30분
        } else if (category.contains("카페")) {
            return LocalTime.of(1, 0); // 1시간
        } else {
            return LocalTime.of(0, 30); // 30분
        }
    }
}