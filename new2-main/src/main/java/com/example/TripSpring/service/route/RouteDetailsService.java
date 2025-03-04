
// src/main/java/com/example/TripSpring/service/route/RouteDetailsService.java
package com.example.TripSpring.service.route;

import com.example.TripSpring.dto.domain.Location;
import com.example.TripSpring.dto.domain.route.GeoPoint;
import com.example.TripSpring.dto.domain.route.TransportMode;
import com.example.TripSpring.dto.route.RouteDetails;
import com.example.TripSpring.dto.route.RouteSegmentDetail;
import com.example.TripSpring.dto.route.RouteStep;
import com.example.TripSpring.dto.transport.TransportOptionDetails;
import com.example.TripSpring.service.TmapService;
import com.example.TripSpring.service.transport.TransportDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteDetailsService {
    private final TransportDetailsService transportDetailsService;
    private final TmapService tmapService;

    public RouteDetails getDetailedRoute(Location start, Location end, TransportMode mode) {
        try {
            // BUS 모드는 임시로 CAR 모드로 처리
            String tmapMode = switch(mode) {
                case WALK -> "WALK";
                case BUS, TAXI -> "CAR";  // BUS는 임시로 CAR로 처리
                default -> "CAR";
            };
    
            Map<String, Object> routeInfo = tmapService.getDetailedRoute(
                start.getLatitude(), start.getLongitude(),
                end.getLatitude(), end.getLongitude(),
                tmapMode
            );
    
            List<RouteSegmentDetail> segments = createRouteSegments(routeInfo);
            return buildRouteDetails(routeInfo, segments, mode);  // 원래 요청한 모드 전달
        } catch (Exception e) {
            log.error("Failed to get route details: {}", e.getMessage());
            throw new RuntimeException("Failed to get route details", e);
        }
    }
    private RouteDetails buildRouteDetails(
        Map<String, Object> routeInfo, 
        List<RouteSegmentDetail> segments,
        TransportMode requestedMode) {
    
        try {
            Map<String, Object> properties = extractProperties(routeInfo);
            
            // 안전한 타입 변환
            double totalDistance = convertToDouble(properties.getOrDefault("totalDistance", 0));
            int totalDuration = convertToInt(properties.getOrDefault("totalTime", 0));
            double cost = calculateCost(requestedMode, totalDistance);
    
            Map<String, Double> metrics = new HashMap<>();
            metrics.put("distance", totalDistance);
            metrics.put("duration", (double) totalDuration);
            metrics.put("cost", cost);
    
            return RouteDetails.builder()
                .segments(segments)
                .totalDistance(totalDistance)
                .totalDuration(totalDuration)
                .metrics(metrics)
                .status("OK")
                .build();
        } catch (Exception e) {
            log.error("Error building route details: {}", e.getMessage());
            throw new RuntimeException("Failed to build route details", e);
        }
    }
    
    // 안전한 타입 변환을 위한 유틸리티 메소드들
    private double convertToDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }
    
    private int convertToInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Double) return ((Double) value).intValue();
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
    
    private Map<String, Object> extractProperties(Map<String, Object> routeInfo) {
        if (routeInfo != null && routeInfo.containsKey("features")) {
            List<Map<String, Object>> features = (List<Map<String, Object>>) routeInfo.get("features");
            if (!features.isEmpty()) {
                return (Map<String, Object>) features.get(0).getOrDefault("properties", new HashMap<>());
            }
        }
        return new HashMap<>();
    }
    
    private double calculateCost(TransportMode mode, double distanceInMeters) {
        return switch (mode) {
            case TAXI -> 3800 + (distanceInMeters / 1000 * 1000); // 기본요금 + km당 1000원
            case BUS -> 1400.0;  // 기본 버스요금
            case SUBWAY -> 1350.0;  // 기본 지하철요금
            default -> 0.0;  // 도보는 비용 없음
        };
    }
    private TransportOptionDetails selectBestOption(
            List<TransportOptionDetails> options, 
            TransportMode preferredMode) {
        return options.stream()
            .filter(opt -> opt.getMode() == preferredMode)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No route found for preferred mode: " + preferredMode));
    }

    private List<RouteSegmentDetail> createRouteSegments(Map<String, Object> routeInfo) {
        List<RouteSegmentDetail> segments = new ArrayList<>();
        List<Map<String, Object>> features = (List<Map<String, Object>>) routeInfo.get("features");
        if (features == null) {
            log.error("No features found in route info");
            return segments;  // 빈 리스트 반환
        }
        
        RouteSegmentDetail.RouteSegmentDetailBuilder currentSegment = null;
        List<RouteStep> currentSteps = new ArrayList<>();
        
        for (Map<String, Object> feature : features) {
            String type = (String) feature.get("type");
            Map<String, Object> properties = (Map<String, Object>) feature.get("properties");
            
            if ("Point".equals(type)) {
                // 회전점, 정류장 등의 포인트 정보 처리
                RouteStep step = createRouteStep(feature);
                currentSteps.add(step);
                
                // 세그먼트 전환점인 경우 새로운 세그먼트 시작
                if (isSegmentTransition(properties)) {
                    if (currentSegment != null) {
                        currentSegment.steps(new ArrayList<>(currentSteps));
                        segments.add(currentSegment.build());
                    }
                    currentSegment = initializeNewSegment(properties);
                    currentSteps.clear();
                }
            } else if ("LineString".equals(type)) {
                // 경로 좌표 처리
                if (currentSegment != null) {
                    currentSegment.path(extractPath(feature));
                }
            }
        }
        
        // 마지막 세그먼트 처리
        if (currentSegment != null) {
            currentSegment.steps(currentSteps);
            segments.add(currentSegment.build());
        }
        
        return segments;
    }

    private RouteStep createRouteStep(Map<String, Object> feature) {
        Map<String, Object> properties = (Map<String, Object>) feature.get("properties");
        Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");
        List<Double> coordinates = (List<Double>) geometry.get("coordinates");
        
        return RouteStep.builder()
            .instruction((String) properties.get("description"))
            .type(parseStepType((String) properties.get("turnType")))
            .location(new GeoPoint(coordinates.get(1), coordinates.get(0)))
            .distanceToNext(((Number) properties.getOrDefault("remainDistance", 0)).doubleValue())
            .additionalInfo((String) properties.get("additionalInfo"))
            .build();
    }

    private RouteStep.StepType parseStepType(String turnType) {
        if (turnType == null) return RouteStep.StepType.STRAIGHT;
        
        return switch (turnType.toUpperCase()) {
            case "LEFT" -> RouteStep.StepType.LEFT;
            case "RIGHT" -> RouteStep.StepType.RIGHT;
            case "UTURN" -> RouteStep.StepType.UTURN;
            case "SLIGHT_LEFT" -> RouteStep.StepType.SLIGHT_LEFT;
            case "SLIGHT_RIGHT" -> RouteStep.StepType.SLIGHT_RIGHT;
            default -> RouteStep.StepType.STRAIGHT;
        };
    }

    private boolean isSegmentTransition(Map<String, Object> properties) {
        String facilityType = (String) properties.get("facilityType");
        return facilityType != null && 
               (facilityType.contains("환승") || facilityType.contains("정류장") || 
                facilityType.contains("역"));
    }

    private RouteSegmentDetail.RouteSegmentDetailBuilder initializeNewSegment(
            Map<String, Object> properties) {
        return RouteSegmentDetail.builder()
            .segmentId(UUID.randomUUID().toString())
            .mode(determineTransportMode(properties))
            .instruction((String) properties.get("description"))
            .distance(((Number) properties.getOrDefault("distance", 0)).doubleValue())
            .duration(((Number) properties.getOrDefault("time", 0)).intValue())
            .congestion(((Number) properties.getOrDefault("congestion", 0)).doubleValue());
    }

    private List<GeoPoint> extractPath(Map<String, Object> feature) {
        List<GeoPoint> path = new ArrayList<>();
        try {
            Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");
            if (geometry != null) {
                List<List<Double>> coordinates = (List<List<Double>>) geometry.get("coordinates");
                if (coordinates != null) {
                    for (List<Double> coord : coordinates) {
                        if (coord.size() >= 2) {
                            path.add(new GeoPoint(coord.get(1), coord.get(0)));
                        }
                    }
                }
            }
        } catch (ClassCastException e) {
            log.error("Error extracting path coordinates: {}", e.getMessage());
        }
        return path;
    }

    private TransportMode determineTransportMode(Map<String, Object> properties) {
        String facilityType = (String) properties.get("facilityType");
        if (facilityType == null) return TransportMode.WALK;
        
        if (facilityType.contains("버스")) return TransportMode.BUS;
        if (facilityType.contains("지하철")) return TransportMode.SUBWAY;
        if (facilityType.contains("택시")) return TransportMode.TAXI;
        
        return TransportMode.WALK;
    }

    private RouteDetails buildRouteDetails(
            TransportOptionDetails option,
            List<RouteSegmentDetail> segments,
            String trafficInfo) {
        
        Map<String, Double> metrics = new HashMap<>();
        metrics.put("distance", option.getDistance());
        metrics.put("duration", (double) option.getDuration());
        metrics.put("cost", option.getCost());
        metrics.put("congestion", option.getCongestion());
        
        List<String> warnings = extractWarnings(trafficInfo);
        
        return RouteDetails.builder()
            .routeId(UUID.randomUUID().toString())
            .segments(segments)
            .metrics(metrics)
            .warnings(warnings)
            .keyPoints(option.getTransitPoints())
            .status(determineRouteStatus(warnings))
            .totalDistance(option.getDistance())
            .totalDuration(option.getDuration())
            .totalCost(option.getCost())
            .build();
    }

    private List<String> extractWarnings(String trafficInfo) {
        List<String> warnings = new ArrayList<>();
        try {
            // TODO: 교통 정보에서 경고 추출 로직 구현
        } catch (Exception e) {
            log.warn("Error extracting warnings from traffic info: {}", e.getMessage());
        }
        return warnings;
    }

    private String determineRouteStatus(List<String> warnings) {
        if (warnings.isEmpty()) return "NORMAL";
        if (warnings.stream().anyMatch(w -> w.contains("통제"))) return "RESTRICTED";
        if (warnings.stream().anyMatch(w -> w.contains("공사"))) return "CONSTRUCTION";
        return "CAUTION";
    }
}