package com.example.TripSpring.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class NaverSearchService {
    private static final String GEOCODE_API_URL = "https://naveropenapi.apigw.ntruss.com/map-geocode/v2/geocode";
    @Value("${app.api.naver.client-id}")
    private String CLIENT_ID;
    @Value("${app.api.naver.client-secret}")
    private String CLIENT_SECRET;

    @SuppressWarnings("unchecked")
    public Map<String, Object> searchPlaces(String query) {
        try {
            String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEOCODE_API_URL + "?query=" + encodedQuery))
                .header("X-NCP-APIGW-API-KEY-ID", CLIENT_ID)
                .header("X-NCP-APIGW-API-KEY", CLIENT_SECRET)
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                return Map.of("message", "API call failed with status: " + response.statusCode());
            }
            
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(response.body(), Map.class);
            
        } catch (Exception e) {
            return Map.of("message", "Failed to search places: " + e.getMessage());
        }
    }
}