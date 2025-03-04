package com.example.TripSpring.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.example.TripSpring.dto.domain.route.TransportMode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.TripSpring.dto.response.navigation.RouteSegment;
import com.example.TripSpring.dto.response.navigation.RouteStep;
import com.example.TripSpring.dto.request.StartNavigationRequest;
import com.example.TripSpring.dto.request.LocationUpdate;
import com.example.TripSpring.dto.response.NavigationStatus;
import com.example.TripSpring.dto.response.navigation.RouteOptionsResponse;
import com.example.TripSpring.dto.response.navigation.RouteDetail;
import com.example.TripSpring.dto.response.navigation.RouteOptionSummary;
import com.example.TripSpring.dto.response.navigation.TransportOption;
import com.example.TripSpring.dto.response.navigation.TotalOptions;
import com.example.TripSpring.service.NavigationService;
import com.example.TripSpring.exception.NavigationException;
import com.example.TripSpring.service.TmapService;
import com.fasterxml.jackson.databind.ObjectMapper;
@Slf4j
@RestController
@RequestMapping("/api/v1/navigation")
@RequiredArgsConstructor
public class NavigationController {
    private final TmapService tmapService;
    private final NavigationService navigationService;

    @PostMapping("/start")
    public ResponseEntity<NavigationStatus> startNavigation(
        @RequestHeader(value = "X-API-KEY", required = true) String apiKey,
        @RequestBody StartNavigationRequest request
    ) {
        try {
            validateNavigationRequest(request);
            NavigationStatus status = navigationService.startNavigation(request);
            return ResponseEntity.ok(status);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid navigation request: {}", e.getMessage());
            throw new NavigationException("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            log.error("Navigation start failed", e);
            String errorMessage = (e.getMessage() != null && !e.getMessage().isEmpty()) 
                ? e.getMessage() 
                : "Internal server error occurred";
            throw new NavigationException("Failed to start navigation: " + errorMessage);
        }
    }
    
    private void validateNavigationRequest(StartNavigationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (request.getCurrentLocation() == null) {
            throw new IllegalArgumentException("Current location is required");
        }
        if (request.getDestination() == null) {
            throw new IllegalArgumentException("Destination is required");
        }
    }
    @PutMapping("/{navigationId}/location")
    public ResponseEntity<NavigationStatus> updateLocation(
            @PathVariable String navigationId,
            @RequestBody LocationUpdate update) {
        try {
            log.debug("Updating location for navigation {}: {}", navigationId, update);
            NavigationStatus status = navigationService.updateLocation(navigationId, update);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Failed to update location for navigation {}", navigationId, e);
            throw new NavigationException("Failed to update location: " + e.getMessage());
        }
    }

    @GetMapping("/{navigationId}/status")
    public ResponseEntity<NavigationStatus> getNavigationStatus(@PathVariable String navigationId) {
        try {
            log.debug("Fetching status for navigation {}", navigationId);
            NavigationStatus status = navigationService.getStatus(navigationId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Failed to get status for navigation {}", navigationId, e);
            throw new NavigationException("Failed to get navigation status: " + e.getMessage());
        }
    }

    @DeleteMapping("/{navigationId}")
    public ResponseEntity<Void> stopNavigation(@PathVariable String navigationId) {
        try {
            log.info("Stopping navigation {}", navigationId);
            navigationService.stopNavigation(navigationId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to stop navigation {}", navigationId, e);
            throw new NavigationException("Failed to stop navigation: " + e.getMessage());
        }
    }

    @GetMapping("/routes")
    public ResponseEntity<RouteOptionsResponse> getRoutes(
        @RequestParam double startLat,
        @RequestParam double startLon, 
        @RequestParam double endLat,
        @RequestParam double endLon
    ) {
        try {
            // 실제 T맵 API 호출하여 도보/자동차 경로 조회
            Map<String, Object> walkingRoute = tmapService.getDetailedRoute(
                startLat, startLon, endLat, endLon, "WALK");
            Map<String, Object> drivingRoute = tmapService.getDetailedRoute(
                startLat, startLon, endLat, endLon, "TAXI");
    
            List<RouteSegment> segments = new ArrayList<>();
    
            // 도보 경로 추가
            if (walkingRoute.containsKey("totalDistance")) {
                double walkDistance = (double) walkingRoute.get("totalDistance");
                int walkTime = (int) walkingRoute.get("totalTime");
                
                segments.add(RouteSegment.builder()
                    .fromLocation("출발지")
                    .toLocation("도착지")
                    .options(Collections.singletonList(
                        TransportOption.builder()
                            .transportMode(TransportMode.WALK)
                            .routes(Collections.singletonList(
                                RouteDetail.builder()
                                    .summary("도보 경로")
                                    .duration(walkTime)
                                    .distance(walkDistance / 1000.0) // m -> km
                                    .cost(0.0)
                                    .steps((List<RouteStep>) walkingRoute.get("turnByTurn"))
                                    .crowdedness("LOW")
                                    .build()
                            ))
                            .build()
                    ))
                    .build());
            }
    
            // 택시 경로 추가
            if (drivingRoute.containsKey("totalDistance")) {
                double driveDistance = (double) drivingRoute.get("totalDistance");
                int driveTime = (int) drivingRoute.get("totalTime");
                double taxiFare = calculateTaxiFare(driveDistance / 1000.0); // m -> km
                
                segments.add(RouteSegment.builder()
                    .fromLocation("출발지")
                    .toLocation("도착지")
                    .options(Collections.singletonList(
                        TransportOption.builder()
                            .transportMode(TransportMode.TAXI)
                            .routes(Collections.singletonList(
                                RouteDetail.builder()
                                    .summary("택시 경로")
                                    .duration(driveTime)
                                    .distance(driveDistance / 1000.0)
                                    .cost(taxiFare)
                                    .steps((List<RouteStep>) drivingRoute.get("turnByTurn"))
                                    .crowdedness(
                                        drivingRoute.containsKey("trafficInfo") ? 
                                        ((Map<String, Object>) drivingRoute.get("trafficInfo"))
                                            .get("status").toString() : "MODERATE"
                                    )
                                    .build()
                            ))
                            .build()
                    ))
                    .build());
            }
    
            // 전체 옵션 분석
            TotalOptions totalOptions = TotalOptions.builder()
                .fastest(findFastestOption(segments))
                .cheapest(findCheapestOption(segments))
                .build();
    
            return ResponseEntity.ok(
                RouteOptionsResponse.builder()
                    .segments(segments)
                    .totalOptions(totalOptions)
                    .build()
            );
    
        } catch (Exception e) {
            log.error("Route calculation failed", e);
            throw new NavigationException("경로 계산 중 오류가 발생했습니다");
        }
    }
    

private RouteSegment buildWalkingSegment(Map<String, Object> apiData) {
    try {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> features = (List<Map<String, Object>>) apiData.get("features");
        Map<String, Object> properties = (Map<String, Object>) features.get(0).get("properties");

        // 숫자형 데이터 안전하게 변환
        double totalDistance = convertToDouble(properties.get("totalDistance")) / 1000.0; // m to km
        int totalTime = convertToInt(properties.get("totalTime")); // minutes
        
        List<RouteStep> steps = extractRouteSteps(features);

        RouteDetail routeDetail = RouteDetail.builder()
            .summary("도보 경로")
            .duration(totalTime)
            .distance(totalDistance)
            .cost(0.0)
            .steps(steps)
            .crowdedness(getCrowdedness(properties))
            .build();

        return RouteSegment.builder()
            .fromLocation(String.valueOf(properties.getOrDefault("startName", "출발지")))
            .toLocation(String.valueOf(properties.getOrDefault("endName", "도착지")))
            .options(Collections.singletonList(
                TransportOption.builder()
                    .transportMode(TransportMode.WALK)
                    .routes(Collections.singletonList(routeDetail))
                    .build()
            ))
            .build();
    } catch (Exception e) {
        log.error("Error building walking segment: {}", e.getMessage());
        throw new NavigationException("도보 경로 생성 중 오류 발생: " + e.getMessage());
    }
}

// 안전한 타입 변환을 위한 유틸리티 메서드들
private double convertToDouble(Object value) {
    if (value == null) return 0.0;
    if (value instanceof Number) {
        return ((Number) value).doubleValue();
    }
    try {
        return Double.parseDouble(String.valueOf(value));
    } catch (NumberFormatException e) {
        return 0.0;
    }
}

private int convertToInt(Object value) {
    if (value == null) return 0;
    if (value instanceof Number) {
        return ((Number) value).intValue();
    }
    try {
        return Integer.parseInt(String.valueOf(value));
    } catch (NumberFormatException e) {
        return 0;
    }
}


private RouteSegment buildTaxiSegment(Map<String, Object> apiData) {
    try {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> features = (List<Map<String, Object>>) apiData.get("features");
        Map<String, Object> properties = (Map<String, Object>) features.get(0).get("properties");

        double totalDistance = convertToDouble(properties.get("totalDistance")) / 1000.0;
        int totalTime = convertToInt(properties.get("totalTime"));
        
        double taxiFare = calculateTaxiFare(totalDistance);

        List<RouteStep> steps = extractRouteSteps(features);

        RouteDetail routeDetail = RouteDetail.builder()
            .summary("택시 경로")
            .duration(totalTime)
            .distance(totalDistance)
            .cost(taxiFare)
            .steps(steps)
            .crowdedness(getCrowdedness(properties))
            .build();

        return RouteSegment.builder()
            .fromLocation(String.valueOf(properties.getOrDefault("startName", "출발지")))
            .toLocation(String.valueOf(properties.getOrDefault("endName", "도착지")))
            .options(Collections.singletonList(
                TransportOption.builder()
                    .transportMode(TransportMode.TAXI)
                    .routes(Collections.singletonList(routeDetail))
                    .build()
            ))
            .build();
    } catch (Exception e) {
        log.error("Error building taxi segment: {}", e.getMessage());
        throw new NavigationException("택시 경로 생성 중 오류 발생: " + e.getMessage());
    }
}

private List<RouteStep> extractRouteSteps(List<Map<String, Object>> features) {
    List<RouteStep> steps = new ArrayList<>();
    
    for (Map<String, Object> feature : features) {
        Map<String, Object> properties = (Map<String, Object>) feature.get("properties");
        if (properties.containsKey("turnType")) {
            steps.add(RouteStep.builder()
                .type(getTurnType(properties))
                .instruction((String) properties.get("description"))
                .duration(extractStepDuration(properties))
                .build());
        }
    }
    
    return steps;
}

private String getCrowdedness(Map<String, Object> properties) {
    double congestion = properties.containsKey("congestion") ? 
        ((Number) properties.get("congestion")).doubleValue() : 0.5;
    
    if (congestion < 0.3) return "LOW";
    if (congestion < 0.7) return "MODERATE";
    return "HIGH";
}

private double calculateTaxiFare(double distanceKm) {
    double baseFare = 3800;  // 기본요금
    double ratePerKm = 1000; // km당 요금
    return baseFare + (distanceKm * ratePerKm);
}


private TotalOptions calculateTotalOptions(List<RouteSegment> segments) {
    RouteOptionSummary fastest = findFastestOption(segments);
    RouteOptionSummary cheapest = findCheapestOption(segments);
    
    return TotalOptions.builder()
        .fastest(fastest)
        .cheapest(cheapest)
        .build();
}

private String extractLocationName(Map<String, Object> properties, String type) {
    Object name = properties.getOrDefault(type + "Name", type.equals("start") ? "출발지" : "도착지");
    return String.valueOf(name);
}
private int extractStepDuration(Map<String, Object> properties) {
    return properties.containsKey("time") ? 
        ((Number) properties.get("time")).intValue() : 0;
}

private String getTurnType(Map<String, Object> properties) {
    String turnType = (String) properties.get("turnType");
    return turnType != null ? turnType : "STRAIGHT";
}

private RouteSegment buildRouteSegment(String from, String to, TransportMode mode, Map<String, Object> routeData) {
   List<RouteDetail> routes = new ArrayList<>();
   // Parse route data and build RouteDetail objects
   routes.add(RouteDetail.builder()
       .summary(extractSummary(routeData))
       .duration(extractDuration(routeData))
       .distance(extractDistance(routeData))
       .cost(calculateCost(mode, extractDistance(routeData)))
       .steps(extractSteps(routeData))
       .crowdedness(extractCrowdedness(routeData))
       .build());

   List<TransportOption> options = new ArrayList<>();
   options.add(TransportOption.builder()
       .transportMode(mode)
       .routes(routes)
       .build());

   return RouteSegment.builder()
       .fromLocation(from)
       .toLocation(to)
       .options(options)
       .build();
}

private TotalOptions buildTotalOptions(List<RouteSegment> segments) {
   RouteSegment fastestSegment = findFastestSegment(segments);
   RouteSegment cheapestSegment = findCheapestSegment(segments);

   return TotalOptions.builder()
       .fastest(buildOptionSummary(fastestSegment))
       .cheapest(buildOptionSummary(cheapestSegment))
       .build();
}

// Helper methods for data extraction...
// Helper methods for data extraction
private String extractSummary(Map<String, Object> routeData) {
   return (String) routeData.getOrDefault("summary", "");
}

private int extractDuration(Map<String, Object> routeData) {
   return ((Number) routeData.getOrDefault("totalTime", 0)).intValue();
}

private double extractDistance(Map<String, Object> routeData) {
   return ((Number) routeData.getOrDefault("totalDistance", 0.0)).doubleValue();
}

private List<RouteStep> extractSteps(Map<String, Object> routeData) {
   List<RouteStep> steps = new ArrayList<>();
   List<Map<String, Object>> features = (List<Map<String, Object>>) routeData.get("features");
   
   if (features != null) {
       for (Map<String, Object> feature : features) {
           Map<String, Object> properties = (Map<String, Object>) feature.get("properties");
           steps.add(RouteStep.builder()
               .type((String) properties.get("turnType"))
               .instruction((String) properties.get("description"))
               .duration(((Number) properties.getOrDefault("time", 0)).intValue())
               .build());
       }
   }
   return steps;
}

private String extractCrowdedness(Map<String, Object> routeData) {
   double congestion = ((Number) routeData.getOrDefault("congestion", 0.5)).doubleValue();
   if (congestion < 0.3) return "LOW";
   if (congestion < 0.7) return "MODERATE";
   return "HIGH";
}

private RouteSegment findFastestSegment(List<RouteSegment> segments) {
   return segments.stream()
       .min(Comparator.comparingInt(segment -> 
           segment.getOptions().get(0).getRoutes().get(0).getDuration()))
       .orElse(segments.get(0));
}

private RouteSegment findCheapestSegment(List<RouteSegment> segments) {
   return segments.stream()
       .min(Comparator.comparingDouble(segment -> 
           segment.getOptions().get(0).getRoutes().get(0).getCost()))
       .orElse(segments.get(0));
}

private RouteOptionSummary buildOptionSummary(RouteSegment segment) {
   RouteDetail route = segment.getOptions().get(0).getRoutes().get(0);
   return RouteOptionSummary.builder()
       .duration(route.getDuration())
       .cost(route.getCost())
       .modes(List.of(segment.getOptions().get(0).getTransportMode().toString()))
       .build();
}

private double calculateCost(TransportMode mode, double distance) {
   return switch (mode) {
       case TAXI -> 3800 + (distance * 1000); // 기본요금 + km당 요금
       case SUBWAY -> 1350; // 기본요금
       case BUS -> 1200; // 기본요금
       default -> 0.0;
   };
}
private void validateCoordinates(double startLat, double startLon, double endLat, double endLon) {
    if (startLat < -90 || startLat > 90 || endLat < -90 || endLat > 90) {
        throw new IllegalArgumentException("위도는 -90에서 90 사이의 값이어야 합니다.");
    }
    if (startLon < -180 || startLon > 180 || endLon < -180 || endLon > 180) {
        throw new IllegalArgumentException("경도는 -180에서 180 사이의 값이어야 합니다.");
    }
}
private RouteOptionSummary findFastestOption(List<RouteSegment> segments) {
    // 가장 짧은 소요시간을 가진 경로 찾기
    RouteSegment fastestSegment = segments.stream()
        .min(Comparator.comparingInt(segment -> 
            segment.getOptions().get(0).getRoutes().get(0).getDuration()))
        .orElseThrow(() -> new NavigationException("경로를 찾을 수 없습니다"));
 
    RouteDetail fastestRoute = fastestSegment.getOptions().get(0).getRoutes().get(0);
    TransportMode fastestMode = fastestSegment.getOptions().get(0).getTransportMode();
 
    return RouteOptionSummary.builder()
        .duration(fastestRoute.getDuration())
        .cost(fastestRoute.getCost())
        .modes(Collections.singletonList(fastestMode.toString()))
        .build();
 }
 
 private RouteOptionSummary findCheapestOption(List<RouteSegment> segments) {
    // 가장 낮은 비용을 가진 경로 찾기 
    RouteSegment cheapestSegment = segments.stream()
        .min(Comparator.comparingDouble(segment -> 
            segment.getOptions().get(0).getRoutes().get(0).getCost()))
        .orElseThrow(() -> new NavigationException("경로를 찾을 수 없습니다"));
 
    RouteDetail cheapestRoute = cheapestSegment.getOptions().get(0).getRoutes().get(0);
    TransportMode cheapestMode = cheapestSegment.getOptions().get(0).getTransportMode();
 
    return RouteOptionSummary.builder()
        .duration(cheapestRoute.getDuration())
        .cost(cheapestRoute.getCost())
        .modes(Collections.singletonList(cheapestMode.toString()))
        .build();
 }
 @PutMapping("/{navigationId}/update")
 public ResponseEntity<NavigationStatus> updateNavigationLocation(
         @PathVariable String navigationId,
         @RequestBody LocationUpdate locationUpdate) {
     
     log.info("Received location update for navigation {}: lat={}, lon={}, speed={}, heading={}",
         navigationId,
         locationUpdate.getLatitude(),
         locationUpdate.getLongitude(),
         locationUpdate.getSpeed(),
         locationUpdate.getHeading()
     );
     
     try {
         NavigationStatus status = navigationService.updateLocation(
             navigationId, 
             locationUpdate
         );
         return ResponseEntity.ok(status);
     } catch (IllegalStateException e) {
         log.warn("Navigation session not found: {}", navigationId);
         return ResponseEntity.notFound().build();
     } catch (Exception e) {
         log.error("Error updating navigation: {}", e.getMessage());
         throw e;
     }
 }

}