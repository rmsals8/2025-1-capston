package com.example.TripSpring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Base64;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.TripSpring.dto.domain.Location;
import com.example.TripSpring.dto.response.navigation.RouteStep;
import com.example.TripSpring.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmapService {
   private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
   private final String apiKey = "7wmYfg7J8X9vVj47sdQO23SZghqYIRFs6OtZOvH9";
   private final String baseUrl = "https://apis.openapi.sk.com";
   public Map<String, Object> getDetailedRoute(
    double startLat, double startLon, 
    double endLat, double endLon,
    String mode) {
try {
    String url;
    if ("WALK".equals(mode)) {
        url = "https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1";
    } else {
        url = "https://apis.openapi.sk.com/tmap/routes?version=1";
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("appKey", apiKey);

    // T-map API 필수 파라미터 추가
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("startX", String.format("%.7f", startLon));
    requestBody.put("startY", String.format("%.7f", startLat));
    requestBody.put("endX", String.format("%.7f", endLon));
    requestBody.put("endY", String.format("%.7f", endLat));
    requestBody.put("reqCoordType", "WGS84GEO");
    requestBody.put("resCoordType", "WGS84GEO");
    requestBody.put("startName", URLEncoder.encode("출발지", "UTF-8"));
    requestBody.put("endName", URLEncoder.encode("도착지", "UTF-8"));

    // 자동차 경로일 경우 추가 파라미터
    if (!"WALK".equals(mode)) {
        requestBody.put("searchOption", "0");  // 최적 경로
    }

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
    ResponseEntity<Map> response = restTemplate.exchange(
        url,
        HttpMethod.POST,
        entity,
        Map.class
    );
    return response.getBody();
} catch (Exception e) {
    log.error("T-map API error: {}", e.getMessage());
    throw new RuntimeException("Failed to get route from T-map API", e);
}
}
    private double extractTotalDistance(JsonNode root) {
        try {
            JsonNode features = root.path("features");
            if (features.size() > 0) {
                JsonNode properties = features.get(0).path("properties");
                return properties.path("totalDistance").asDouble() / 1000.0; // meters to km
            }
        } catch (Exception e) {
            log.error("Error extracting total distance", e);
        }
        return 0.0;
    }

    private int extractTotalTime(JsonNode root) {
        try {
            JsonNode features = root.path("features");
            if (features.size() > 0) {
                JsonNode properties = features.get(0).path("properties");
                return properties.path("totalTime").asInt();
            }
        } catch (Exception e) {
            log.error("Error extracting total time", e);
        }
        return 0;
    }

    private List<Map<String, Object>> extractTurnByTurn(JsonNode root) {
        List<Map<String, Object>> turnByTurn = new ArrayList<>();
        try {
            JsonNode features = root.path("features");
            for (JsonNode feature : features) {
                if ("Point".equals(feature.path("geometry").path("type").asText())) {
                    Map<String, Object> point = new HashMap<>();
                    JsonNode properties = feature.path("properties");
                    JsonNode geometry = feature.path("geometry");
                    
                    point.put("description", properties.path("description").asText());
                    point.put("distance", properties.path("distance").asDouble());
                    point.put("name", properties.path("name").asText());
                    point.put("latitude", geometry.path("coordinates").get(1).asDouble());
                    point.put("longitude", geometry.path("coordinates").get(0).asDouble());
                    
                    turnByTurn.add(point);
                }
            }
        } catch (Exception e) {
            log.error("Error extracting turn by turn directions", e);
        }
        return turnByTurn;
    }

    private Map<String, Object> extractTrafficInfo(JsonNode root) {
        Map<String, Object> trafficInfo = new HashMap<>();
        try {
            JsonNode features = root.path("features");
            if (features.size() > 0) {
                JsonNode properties = features.get(0).path("properties");
                trafficInfo.put("congestion", properties.path("congestion").asDouble());
                trafficInfo.put("speed", properties.path("speed").asDouble());
            }
        } catch (Exception e) {
            log.error("Error extracting traffic info", e);
        }
        return trafficInfo;
    }

    public String getRealTimeTraffic(double lat, double lon) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/tmap/traffic/realtime")
                .queryParam("appKey", apiKey)
                .queryParam("lat", String.format("%.7f", lat))
                .queryParam("lon", String.format("%.7f", lon))
                .queryParam("radius", "1")
                .build()
                .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("appKey", apiKey);

            log.debug("Requesting traffic info - URL: {}", url);
            
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.debug("Traffic API Response: {}", response.getBody());
                return response.getBody();
            }
            return createDefaultTrafficInfo();
        } catch (Exception e) {
            log.error("Error getting real-time traffic: {}", e.getMessage());
            return createDefaultTrafficInfo();
        }
    }

    private Map<String, Object> createDefaultRouteInfo() {
        Map<String, Object> defaultInfo = new HashMap<>();
        defaultInfo.put("totalDistance", 0.0);
        defaultInfo.put("totalTime", 0);
        defaultInfo.put("turnByTurn", new ArrayList<>());
        try {
            defaultInfo.put("trafficInfo", objectMapper.readTree(createDefaultTrafficInfo()));
        } catch (Exception e) {
            defaultInfo.put("trafficInfo", new HashMap<>());
        }
        return defaultInfo;
    }

    public String getWalkingRoute(Double startLat, Double startLon, Double endLat, Double endLon) {
        try {
            String url = baseUrl + "/tmap/routes/pedestrian";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("appKey", apiKey);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("startX", String.format("%.7f", startLon));
            requestBody.put("startY", String.format("%.7f", startLat));
            requestBody.put("endX", String.format("%.7f", endLon));
            requestBody.put("endY", String.format("%.7f", endLat));
            requestBody.put("reqCoordType", "WGS84GEO");
            requestBody.put("resCoordType", "WGS84GEO");
            requestBody.put("startName", URLEncoder.encode("출발지", StandardCharsets.UTF_8));
            requestBody.put("endName", URLEncoder.encode("도착지", StandardCharsets.UTF_8));
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
    
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
            );
    
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }
            
            throw new RuntimeException("Failed to get walking route");
        } catch (Exception e) {
            log.error("Failed to get walking route from T-map API: {}", e.getMessage());
            throw new RuntimeException("Failed to get walking route", e);
        }
    }
    private String getTrafficStatus(JsonNode properties) {
        double congestion = properties.path("congestion").asDouble(0.5);
        if (congestion < 0.3) return "LOW";
        if (congestion < 0.7) return "MODERATE";
        return "HIGH";
     }
// TmapService.java
public String getTrafficInfo(Double lat, Double lon) {
    String url = "https://apis.openapi.sk.com/tmap/traffic";
    
    HttpHeaders headers = new HttpHeaders();
    headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
    headers.set("appKey", apiKey);
    
    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
        .queryParam("version", "1")
        .queryParam("centerLat", lat)
        .queryParam("centerLon", lon)
        .queryParam("reqCoordType", "WGS84GEO")
        .queryParam("zoomLevel", "14")
        .queryParam("trafficType", "AUTO");
    
    HttpEntity<?> entity = new HttpEntity<>(headers);
    
    ResponseEntity<String> response = restTemplate.exchange(
        builder.toUriString(),
        HttpMethod.GET,
        entity,
        String.class
    );
    
    return response.getBody();
}
    private String encodeParameters(MultiValueMap<String, String> params) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        
        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            for (String value : entry.getValue()) {
                if (!first) {
                    result.append("&");
                }
                result.append(entry.getKey()).append("=").append(value);
                first = false;
            }
        }
        
        return result.toString();
    }
    private String createDefaultTrafficInfo() {
        try {
            Map<String, Object> defaultInfo = new HashMap<>();
            defaultInfo.put("trafficInfo", Map.of(
                "status", "OK",
                "estimatedTime", 0,
                "congestion", 0.5
            ));
            return new org.json.JSONObject(defaultInfo).toString();
        } catch (Exception e) {
            log.error("Error creating default traffic info", e);
            return "{}";
        }
    }

    public String getRoute(Location start, Location end) {
        String url = "https://apis.openapi.sk.com/tmap/routes?version=1";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("appKey", apiKey);
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("startX", String.valueOf(start.getLongitude()));
        params.add("startY", String.valueOf(start.getLatitude()));
        params.add("endX", String.valueOf(end.getLongitude()));
        params.add("endY", String.valueOf(end.getLatitude()));
        params.add("reqCoordType", "WGS84GEO");
        
        HttpEntity<MultiValueMap<String, String>> entity = 
            new HttpEntity<>(params, headers);
        
        return restTemplate.postForObject(url, entity, String.class);
    }

    // RouteSementAnalyer  의 getWalkingRoute 오류 고쳐야함 
// public Map<String, Object> getWalkingRoute(Double startLat, Double startLon, Double endLat, Double endLon) {
//     try {
//         String url = baseUrl + "/tmap/routes/pedestrian";
        
//         HttpHeaders headers = new HttpHeaders();
//         headers.setContentType(MediaType.APPLICATION_JSON);
//         headers.set("appKey", apiKey);
        
//         // 요청 바디 구성
//         Map<String, Object> requestBody = new HashMap<>();
//         requestBody.put("startX", String.format("%.7f", startLon));
//         requestBody.put("startY", String.format("%.7f", startLat));
//         requestBody.put("endX", String.format("%.7f", endLon));
//         requestBody.put("endY", String.format("%.7f", endLat));
//         requestBody.put("reqCoordType", "WGS84GEO");
//         requestBody.put("resCoordType", "WGS84GEO");
//         requestBody.put("startName", "출발지");
//         requestBody.put("endName", "도착지");

//         HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

//         ResponseEntity<String> response = restTemplate.exchange(
//             url,
//             HttpMethod.POST,
//             entity,
//             String.class
//         );

//         if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
//             ObjectMapper mapper = new ObjectMapper();
//             return mapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {});
//         }

//         return createDefaultRouteResponse("WALK");
//     } catch (Exception e) {
//         log.error("Failed to get walking route", e);
//         return createDefaultRouteResponse("WALK");
//     }
// }   
    private Map<String, Object> createDefaultRouteResponse(String mode) {
        Map<String, Object> response = new HashMap<>();
        response.put("totalDistance", mode.equals("WALK") ? 1000.0 : 2000.0);  // 1km or 2km
        response.put("totalTime", mode.equals("WALK") ? 15 : 10);  // 15min or 10min
        response.put("turnByTurn", Collections.emptyList());
        
        if (!mode.equals("WALK")) {
            Map<String, Object> trafficInfo = new HashMap<>();
            trafficInfo.put("status", "MODERATE");
            trafficInfo.put("congestion", 0.5);
            response.put("trafficInfo", trafficInfo);
        }
        
        return response;
     }

public String getTransitRoute(Double startLat, Double startLon, Double endLat, Double endLon) {
    try {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("appKey", apiKey);  // apiKey 설정

        String url = baseUrl + "/tmap/routes/transit";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("startX", String.format("%.7f", startLon));
        requestBody.put("startY", String.format("%.7f", startLat));
        requestBody.put("endX", String.format("%.7f", endLon));
        requestBody.put("endY", String.format("%.7f", endLat));
        requestBody.put("reqCoordType", "WGS84GEO");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            entity,
            String.class
        );

        return response.getBody();
    } catch (Exception e) {
        log.error("Failed to get transit route", e);
        throw new RuntimeException("Failed to get transit route");
    }
}
    public String getDrivingRoute(Double startLat, Double startLon, Double endLat, Double endLon) {
        try {
            validateCoordinates(startLat, startLon);
            validateCoordinates(endLat, endLon);

            String url = baseUrl + "/tmap/routes";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("appKey", apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("startX", String.format("%.7f", startLon));
            requestBody.put("startY", String.format("%.7f", startLat));
            requestBody.put("endX", String.format("%.7f", endLon));
            requestBody.put("endY", String.format("%.7f", endLat));
            requestBody.put("reqCoordType", "WGS84GEO");
            requestBody.put("resCoordType", "WGS84GEO");
            requestBody.put("searchOption", "0");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to get driving route information");
            }
        } catch (Exception e) {
            log.error("Failed to get driving route from T-map API: {}", e.getMessage());
            throw new RuntimeException("Failed to get driving route information: " + e.getMessage());
        }
    }

    private void validateCoordinates(Double lat, Double lon) {
        if (lat == null || lon == null) {
            throw new IllegalArgumentException("Coordinates cannot be null");
        }
        if (lat < -90 || lat > 90) {
            throw new IllegalArgumentException("Invalid latitude: " + lat);
        }
        if (lon < -180 || lon > 180) {
            throw new IllegalArgumentException("Invalid longitude: " + lon);
        }
    }
}