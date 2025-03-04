package com.example.TripSpring.provider;

import com.example.TripSpring.dto.domain.PlaceInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class FoursquarePlaceSearchProvider implements PlaceSearchProvider {
    private final RestTemplate restTemplate;
    @Value("${app.api.foursquare}")
    private String apiKey;
    private final String baseUrl = "https://api.foursquare.com/v3";

    @Override
    public PlaceInfo searchPlace(String placeName, double lat, double lng) {
        try {
            String searchUrl = baseUrl + "/places/search";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(searchUrl)
                .queryParam("query", placeName)
                .queryParam("ll", lat + "," + lng)
                .queryParam("limit", 1);

            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> searchResponse = 
                restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
                );

            Map<String, Object> responseBody = searchResponse.getBody();
            if (responseBody != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> results = (List<Map<String, Object>>) responseBody.get("results");
                if (results != null && !results.isEmpty()) {
                    return convertToPlaceInfo(results.get(0));
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Foursquare API failed for place {}: {}", placeName, e.getMessage());
            return null;
        }
    }

    private PlaceInfo convertToPlaceInfo(Map<String, Object> data) {
        String id = (String) data.get("fsq_id");
        String name = (String) data.get("name");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> hours = data.containsKey("hours") ? 
            (Map<String, Object>) data.get("hours") : new HashMap<>();
        
        @SuppressWarnings("unchecked")    
        Map<String, Object> location = data.containsKey("location") ? 
            (Map<String, Object>) data.get("location") : new HashMap<>();
            
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> categories = data.containsKey("categories") ? 
            (List<Map<String, Object>>) data.get("categories") : new ArrayList<>();

        return new PlaceInfo(
            id,
            name,
            getBooleanValue(hours, "is_open"),
            parseTime(getStringValue(hours, "opening_hours")),
            parseTime(getStringValue(hours, "closing_hours")),
            getDoubleValue(data, "current_popularity", 0.5),
            getStringValue(location, "formatted_address"),
            getStringValue(data, "phone"),
            categories.isEmpty() ? "" : getStringValue(categories.get(0), "name"),
            getDoubleValue(data, "rating", 0.0),
            LocalTime.of(1, 0)
        );
    }

    private String getStringValue(Map<String, Object> map, String key) {
        return map != null ? (String) map.getOrDefault(key, "") : "";
    }

    private boolean getBooleanValue(Map<String, Object> map, String key) {
        return map != null && Boolean.TRUE.equals(map.get(key));
    }

    private double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
        if (map != null && map.get(key) instanceof Number) {
            return ((Number) map.get(key)).doubleValue();
        }
        return defaultValue;
    }

    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return LocalTime.of(0, 0);
        }
        try {
            return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HHmm"));
        } catch (Exception e) {
            log.error("Failed to parse time string: {}", timeStr);
            return LocalTime.of(0, 0);
        }
    }
}