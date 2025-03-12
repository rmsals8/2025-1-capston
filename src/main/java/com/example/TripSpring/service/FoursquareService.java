package com.example.TripSpring.service;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.springframework.cache.annotation.Cacheable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.TripSpring.dto.foursquare.FoursquarePlaceDetails;
import com.example.TripSpring.dto.foursquare.FoursquareResponse;
import com.example.TripSpring.exception.ApiException;
import java.net.URLEncoder;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
@Slf4j
@Service
public class FoursquareService {
   private static final String BASE_URL = "https://api.foursquare.com/v3";
   private final RestTemplate restTemplate;
   @Value("${app.api.foursquare}")
   private String apiKey;

   @Autowired
   public FoursquareService(RestTemplate restTemplate) {
       this.restTemplate = restTemplate;
   }

    @Retryable(
        value = {ApiException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000)
    )
    public FoursquareResponse searchPlaces(String query, double lat, double lng, int radius) {
        try {
            HttpHeaders headers = createHeaders();
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            
            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/places/search")
                .queryParam("query", encodedQuery)
                .queryParam("ll", String.format("%f,%f", lat, lng))
                .queryParam("radius", radius)
                .build()
                .toUriString();

            log.debug("Searching places with query: {}, url: {}", query, url);

            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<FoursquareResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                FoursquareResponse.class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new ApiException("Failed to search places");
            }

            return response.getBody();

        } catch (Exception e) {
            log.error("Error searching places: {}", e.getMessage());
            throw new ApiException("Failed to search places: " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "placeDetails", key = "#placeId")
    @Retryable(
        value = {ApiException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000)
    )
    public FoursquarePlaceDetails getPlaceDetailsById(String placeId) {
        try {
            HttpHeaders headers = createHeaders();
            String url = String.format("%s/places/%s", BASE_URL, placeId);

            log.debug("Getting place details for ID: {}", placeId);

            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<FoursquarePlaceDetails> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                FoursquarePlaceDetails.class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new ApiException("Failed to get place details");
            }

            return response.getBody();

        } catch (Exception e) {
            log.error("Error getting place details for ID {}: {}", placeId, e.getMessage());
            throw new ApiException("Failed to get place details: " + e.getMessage(), e);
        }
    }

    /**
     * 장소명으로 상세 정보 조회 (기존 메소드는 하위 호환성을 위해 유지)
     */
    @Deprecated
    public FoursquarePlaceDetails getPlaceDetails(String placeName) {
        try {
            // 1. 먼저 장소 검색
            FoursquareResponse searchResponse = searchPlaces(
                placeName,
                0.0, // 기본 좌표값 - 실제 구현시 적절한 중심점 필요
                0.0,
                1000
            );

            // 2. 검색 결과가 있으면 첫 번째 결과의 ID로 상세 정보 조회
            if (searchResponse != null && 
                searchResponse.getResults() != null && 
                !searchResponse.getResults().isEmpty()) {
                
                String placeId = searchResponse.getResults().get(0).getFsq_id();
                return getPlaceDetailsById(placeId);
            }

            return null;

        } catch (Exception e) {
            log.error("Error getting place details for name {}: {}", placeName, e.getMessage());
            return null;
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }
}