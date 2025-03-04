package com.example.TripSpring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class NaverPlaceSearchService {
    private static final String LOCAL_API_URL = "https://openapi.naver.com/v1/search/local.json";
    @Value("${app.api.naver.client-id}")
    private String CLIENT_ID;
    @Value("${app.api.naver.client-secret}")
    private String CLIENT_SECRET;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    @Autowired
    public NaverPlaceSearchService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    public Map<String, Object> searchPlaces(String query) {
        try {
            // URL 인코딩
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

            // HttpClient 생성
            HttpClient client = HttpClient.newHttpClient();

            // HTTP 요청 생성
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(LOCAL_API_URL + "?query=" + encodedQuery + "&display=5"))
                .header("X-Naver-Client-Id", CLIENT_ID)
                .header("X-Naver-Client-Secret", CLIENT_SECRET)
                .header("Content-Type", "application/json")
                .GET()
                .build();

            // 요청 전송 및 응답 수신
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("API Response Status: {}", response.statusCode());
            log.info("API Response Body: {}", response.body());

            // 응답 코드 확인
            if (response.statusCode() != 200) {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> errorMap = mapper.readValue(response.body(), Map.class);
                
                return Map.of(
                    "status", "ERROR", 
                    "message", errorMap.getOrDefault("errorMessage", "Unknown error"),
                    "errorCode", errorMap.getOrDefault("errorCode", "UNKNOWN")
                );
            }

            // JSON 파싱
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> resultMap = mapper.readValue(response.body(), Map.class);

            return resultMap;

        } catch (Exception e) {
            log.error("검색 중 오류 발생", e);
            return Map.of(
                "status", "ERROR", 
                "message", "검색 중 오류 발생: " + e.getMessage()
            );
        }
    }

    // 대안적인 API 호출 방식 (필요시 사용)
    public Map<String, Object> searchPlacesAlternative(String query) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            
            // 네이버 개발자 센터에서 확인한 최신 인증 방식 적용
            HttpClient client = HttpClient.newHttpClient();
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(LOCAL_API_URL + "?query=" + encodedQuery + "&display=5"))
                .header("Accept", "application/json")
                .header("X-Naver-Client-Id", CLIENT_ID)
                .header("X-Naver-Client-Secret", CLIENT_SECRET)
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            log.info("Alternative API Response Status: {}", response.statusCode());
            log.info("Alternative API Response Body: {}", response.body());

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(response.body(), Map.class);

        } catch (Exception e) {
            log.error("대체 검색 중 오류 발생", e);
            return Map.of(
                "status", "ERROR", 
                "message", "대체 검색 중 오류 발생: " + e.getMessage()
            );
        }
    }
    public Map<String, Object> searchPlace(String query) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = LOCAL_API_URL + "?query=" + encodedQuery;

            var headers = new org.springframework.http.HttpHeaders();
            headers.set("X-NCP-APIGW-API-KEY-ID", CLIENT_ID);
            headers.set("X-NCP-APIGW-API-KEY", CLIENT_SECRET);

            var entity = new org.springframework.http.HttpEntity<>(headers);

            var response = restTemplate.exchange(
                url,
                org.springframework.http.HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode() == org.springframework.http.HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                var addresses = (java.util.List<Map<String, Object>>) responseMap.get("addresses");
                
                if (addresses != null && !addresses.isEmpty()) {
                    var firstAddress = addresses.get(0);
                    return Map.of(
                        "latitude", firstAddress.get("y"),
                        "longitude", firstAddress.get("x"),
                        "address", firstAddress.get("roadAddress")
                    );
                }
            }

            // 기본값 반환 (서울시청 좌표)
            return Map.of(
                "latitude", "37.5665",
                "longitude", "126.9780",
                "address", "서울특별시 중구 세종대로 110"
            );

        } catch (Exception e) {
            log.error("Failed to search place: {}", e.getMessage(), e);
            return Map.of(
                "latitude", "37.5665",
                "longitude", "126.9780",
                "address", "서울특별시 중구 세종대로 110"
            );
        }
    }
}