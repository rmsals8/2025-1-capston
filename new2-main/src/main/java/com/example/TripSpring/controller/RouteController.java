//src/main/java/com/example/TripSpring/controller/RouteController.java
package com.example.TripSpring.controller;

import com.example.TripSpring.dto.domain.Location;
import com.example.TripSpring.dto.domain.route.TransportMode;
import com.example.TripSpring.dto.request.OptimizeRequest;
import com.example.TripSpring.dto.response.OptimizeResponse;
import com.example.TripSpring.dto.route.*;
import com.example.TripSpring.service.route.MultiRouteGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
@Tag(name = "Route API", description = "경로 관련 API")
public class RouteController {
    private final MultiRouteGenerationService routeService;

    @Operation(summary = "경로 최적화", description = "주어진 경유지와 조건에 따른 최적 경로를 제공합니다")
    @PostMapping("/optimize")
    public ResponseEntity<OptimizeResponse> optimizeRoutes(
            @RequestBody @Validated OptimizeRequest request,
            @RequestParam(required = false) List<String> optimizationCriteria,
            @RequestParam(required = false) List<String> transportModes,
            @RequestParam(required = false, defaultValue = "3") int maxResults) {
        
        try {
            log.info("Route optimization request received: {}", request);
            
            // 1. 경로 선호도 설정 생성
            RoutePreferences prefs = createRoutePreferences(
                optimizationCriteria,
                transportModes,
                request.getConstraints()
            );
            
            // 2. 경로 계산
            List<CompleteRoute> routes = routeService.generateMultipleRoutes(
                request.getWaypoints(),
                prefs
            );
            
            // 3. 응답 생성
            OptimizeResponse response = createOptimizeResponse(routes, maxResults);
            
            log.info("Route optimization completed. Found {} routes", routes.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error optimizing routes: {}", e.getMessage());
            throw new RuntimeException("Failed to optimize routes", e);
        }
    }

        private RoutePreferences createRoutePreferences(
            List<String> optimizationCriteria,
            List<String> transportModes,
            OptimizeRequest.OptimizeConstraints constraints) {
        
        // 1. 최적화 기준 파싱
        boolean minimizeTime = hasOptimizationCriteria(optimizationCriteria, "TIME");
        boolean minimizeCost = hasOptimizationCriteria(optimizationCriteria, "COST");
        boolean minimizeTransfers = hasOptimizationCriteria(optimizationCriteria, "TRANSFERS");
        
        // 2. 이동수단 파싱
        Set<TransportMode> preferredModes = parseTransportModes(transportModes);
        
        // 3. 제약조건 설정
        return RoutePreferences.builder()
            .preferredModes(preferredModes)
            .minimizeTime(minimizeTime)
            .minimizeCost(minimizeCost)
            .minimizeTransfers(minimizeTransfers)
            .maxWalkingDistance(constraints != null ? 
                constraints.getMaxWalkingDistance() : 1000)
            .maxCost(constraints != null ? 
                constraints.getMaxCost() : null)
            .maxDuration(constraints != null ? 
                constraints.getMaxDuration() : null)
            .build();
    }

    private boolean hasOptimizationCriteria(List<String> criteria, String target) {
        return criteria != null && criteria.stream()
            .anyMatch(c -> c.equalsIgnoreCase(target));
    }

    private Set<TransportMode> parseTransportModes(List<String> modes) {
        if (modes == null || modes.isEmpty()) {
            return EnumSet.allOf(TransportMode.class);
        }
        
        Set<TransportMode> result = EnumSet.noneOf(TransportMode.class);
        for (String mode : modes) {
            try {
                result.add(TransportMode.valueOf(mode.toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid transport mode: {}", mode);
            }
        }
        
        return result.isEmpty() ? 
            EnumSet.allOf(TransportMode.class) : result;
    }

    private OptimizeResponse createOptimizeResponse(
            List<CompleteRoute> routes,
            int maxResults) {
        
        // 1. 경로 필터링
        List<CompleteRoute> filteredRoutes = routes.stream()
            .limit(maxResults)
            .collect(Collectors.toList());
        
        // 2. 이동수단 분포 계산
        Map<String, Integer> modeDistribution = calculateModeDistribution(filteredRoutes);
        
        // 3. 메트릭스 생성
        OptimizeResponse.OptimizeMetrics metrics = OptimizeResponse.OptimizeMetrics.builder()
            .totalRoutesFound(routes.size())
            .filteredRoutes(filteredRoutes.size())
            .averageScore(calculateAverageScore(filteredRoutes))
            .transportModeDistribution(modeDistribution)
            .build();
        
        // 4. 요약 정보 생성
        Map<String, Object> summary = createSummary(filteredRoutes);
        
        return OptimizeResponse.builder()
            .routes(filteredRoutes)
            .metrics(metrics)
            .summary(summary)
            .build();
    }

    private Map<String, Integer> calculateModeDistribution(List<CompleteRoute> routes) {
        Map<String, Integer> distribution = new HashMap<>();
        
        routes.forEach(route -> 
            route.getOptions().forEach(option -> 
                option.getParts().forEach(part -> {
                    String mode = part.getMode().name();
                    distribution.merge(mode, 1, Integer::sum);
                })
            )
        );
        
        return distribution;
    }

    private double calculateAverageScore(List<CompleteRoute> routes) {
        return routes.stream()
            .mapToDouble(CompleteRoute::getScore)
            .average()
            .orElse(0.0);
    }

    private Map<String, Object> createSummary(List<CompleteRoute> routes) {
        Map<String, Object> summary = new HashMap<>();
        
        // 경로가 없는 경우 기본 응답 반환
        if (routes == null || routes.isEmpty()) {
            return Map.of(
                "status", "NO_ROUTES_FOUND",
                "message", "사용 가능한 경로가 없습니다."
            );
        }
    
        // 최단 시간 경로
        routes.stream()
            .filter(route -> route.getOptions() != null && !route.getOptions().isEmpty())
            .min(Comparator.comparingInt(r -> r.getOptions().get(0).getTotalDuration()))
            .ifPresent(r -> summary.put("fastestRoute", mapRouteToSummary(r)));
        
        // 최저 비용 경로
        routes.stream()
            .filter(route -> route.getOptions() != null && !route.getOptions().isEmpty())
            .min(Comparator.comparingDouble(r -> r.getOptions().get(0).getTotalCost()))
            .ifPresent(r -> summary.put("cheapestRoute", mapRouteToSummary(r)));
        
        // 최소 환승 경로
        routes.stream()
            .filter(route -> route.getOptions() != null && !route.getOptions().isEmpty())
            .min(Comparator.comparingInt(r -> r.getOptions().get(0).getNumberOfTransfers()))
            .ifPresent(r -> summary.put("leastTransfersRoute", mapRouteToSummary(r)));
        
        return summary;
    }
    
    private Map<String, Object> mapRouteToSummary(CompleteRoute route) {
        if (route == null || route.getOptions() == null || route.getOptions().isEmpty()) {
            return Map.of(
                "status", "INVALID_ROUTE",
                "message", "경로 정보가 유효하지 않습니다."
            );
        }
    
        RouteOption firstOption = route.getOptions().get(0);
        
        return Map.of(
            "duration", firstOption.getTotalDuration(),
            "cost", firstOption.getTotalCost(),
            "transfers", firstOption.getNumberOfTransfers(),
            "transportModes", firstOption.getParts().stream()
                .map(part -> part.getMode().name())
                .collect(Collectors.toList())
        );
    }


}