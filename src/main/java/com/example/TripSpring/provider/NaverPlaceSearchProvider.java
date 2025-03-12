package com.example.TripSpring.provider;

import com.example.TripSpring.dto.domain.PlaceInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
public class NaverPlaceSearchProvider implements PlaceSearchProvider {
    private final RestTemplate restTemplate;
    @Value("${app.api.naver.client-id}")
    private String CLIENT_ID;
    
    @Value("${app.api.naver.client-secret}")
    private String CLIENT_SECRET;
    private final String SEARCH_URL = "https://openapi.naver.com/v1/search/local.json";

    @Override
    @SuppressWarnings("unchecked")
    public PlaceInfo searchPlace(String placeName, double lat, double lng) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Naver-Client-Id", CLIENT_ID);
            headers.set("X-Naver-Client-Secret", CLIENT_SECRET);

            String url = UriComponentsBuilder.fromHttpUrl(SEARCH_URL)
                .queryParam("query", placeName)
                .queryParam("display", 1)
                .build()
                .toUriString();

            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );

            if (response.getBody() != null && response.getBody().containsKey("items")) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) response.getBody().get("items");
                if (!items.isEmpty()) {
                    return convertToPlaceInfo(items.get(0));
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Naver search API failed for place {}: {}", placeName, e.getMessage());
            return null;
        }
    }

    private PlaceInfo convertToPlaceInfo(Map<String, Object> item) {
        return new PlaceInfo(
            String.valueOf(item.get("id")),
            (String) item.get("title"),
            true, // 기본값
            LocalTime.of(9, 0), // 기본 영업시작시간
            LocalTime.of(22, 0), // 기본 영업종료시간
            0.5, // 기본 혼잡도
            (String) item.get("roadAddress"),
            (String) item.get("telephone"),
            (String) item.get("category"),
            parseRating(item.get("rating")),
            LocalTime.of(1, 0) // 기본 방문시간
        );
    }

    private double parseRating(Object rating) {
        try {
            return rating != null ? Double.parseDouble(rating.toString()) : 0.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}