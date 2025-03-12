//src/main/java/com/example/TripSpring/service/route/MultiModalRouteService.java
package com.example.TripSpring.service.route;

import com.example.TripSpring.dto.domain.Location;
import com.example.TripSpring.dto.domain.route.TransportMode;
import com.example.TripSpring.dto.route.*;
import com.example.TripSpring.service.transport.TransportDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiModalRouteService {
    private final TransportDetailsService transportDetailsService;
    private final RouteDetailsService routeDetailsService;
    
    private static final int MAX_COMBINATIONS = 5;  // 최대 조합 수
    private static final int MAX_TRANSFERS = 3;     // 최대 환승 횟수

    public List<RouteOption> calculateMultiModalRoutes(
            Location start, 
            Location end,
            RoutePreferences prefs) {
        try {
            // 1. 단일 수단 경로 계산
            List<RouteOption> singleModeRoutes = calculateSingleModeRoutes(start, end, prefs);
            
            // 2. 복합 수단 경로 계산
            List<RouteOption> mixedModeRoutes = calculateMixedModeRoutes(start, end, prefs);
            
            // 3. 모든 경로 통합 및 점수 계산
            List<RouteOption> allRoutes = new ArrayList<>();
            allRoutes.addAll(singleModeRoutes);
            allRoutes.addAll(mixedModeRoutes);
            
            // 4. 점수 기반 정렬 및 필터링
            return scoreAndFilterRoutes(allRoutes, prefs);
            
        } catch (Exception e) {
            log.error("Error calculating multi-modal routes: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate routes", e);
        }
    }

    private List<RouteOption> calculateSingleModeRoutes(
            Location start,
            Location end,
            RoutePreferences prefs) {
        List<RouteOption> options = new ArrayList<>();
        
        for (TransportMode mode : prefs.getPreferredModes()) {
            RouteDetails details = routeDetailsService.getDetailedRoute(start, end, mode);
            
            // 경로 제약조건 체크
            if (!isValidRoute(details, prefs)) {
                continue;
            }
            
            // 단일 수단 경로 생성
            RoutePart part = RoutePart.builder()
                .start(start)
                .end(end)
                .mode(mode)
                .distance(details.getTotalDistance())
                .duration(details.getTotalDuration())
                .cost(details.getMetrics().get("cost"))
                .instruction(generateInstruction(mode, start, end))
                .build();
            
            RouteOption option = RouteOption.builder()
                .optionId(UUID.randomUUID().toString())
                .parts(Collections.singletonList(part))
                .totalDistance(details.getTotalDistance())
                .totalDuration(details.getTotalDuration())
                .totalCost(details.getMetrics().get("cost"))
                .numberOfTransfers(0)
                .description(generateDescription(Collections.singletonList(part)))
                .build();
            
            options.add(option);
        }
        
        return options;
    }

    private List<RouteOption> calculateMixedModeRoutes(
            Location start,
            Location end,
            RoutePreferences prefs) {
        List<RouteOption> options = new ArrayList<>();
        Set<TransportMode> modes = prefs.getPreferredModes();
        
        // 중간 지점들 생성
        List<Location> transitPoints = generateTransitPoints(start, end);
        
        for (Location transitPoint : transitPoints) {
            for (TransportMode firstMode : modes) {
                for (TransportMode secondMode : modes) {
                    // 같은 이동수단은 건너뛰기
                    if (firstMode == secondMode) continue;
                    
                    // 두 구간의 경로 계산
                    RouteDetails firstLeg = routeDetailsService.getDetailedRoute(
                        start, transitPoint, firstMode);
                    RouteDetails secondLeg = routeDetailsService.getDetailedRoute(
                        transitPoint, end, secondMode);
                    
                    // 제약조건 체크
                    if (!isValidRoute(firstLeg, prefs) || !isValidRoute(secondLeg, prefs)) {
                        continue;
                    }
                    
                    // 복합 경로 생성
                    List<RoutePart> parts = Arrays.asList(
                        createRoutePart(start, transitPoint, firstMode, firstLeg),
                        createRoutePart(transitPoint, end, secondMode, secondLeg)
                    );
                    
                    double totalDistance = firstLeg.getTotalDistance() + secondLeg.getTotalDistance();
                    int totalDuration = firstLeg.getTotalDuration() + secondLeg.getTotalDuration();
                    double totalCost = firstLeg.getMetrics().get("cost") + 
                                     secondLeg.getMetrics().get("cost");
                    
                    RouteOption option = RouteOption.builder()
                        .optionId(UUID.randomUUID().toString())
                        .parts(parts)
                        .totalDistance(totalDistance)
                        .totalDuration(totalDuration)
                        .totalCost(totalCost)
                        .numberOfTransfers(1)
                        .description(generateDescription(parts))
                        .build();
                    
                    options.add(option);
                }
            }
        }
        
        return options;
    }

    private boolean isValidRoute(RouteDetails route, RoutePreferences prefs) {
        // 최대 도보 거리 체크
        if (route.getSegments().stream()
                .anyMatch(s -> s.getMode() == TransportMode.WALK && 
                              s.getDistance() > prefs.getMaxWalkingDistance())) {
            return false;
        }
        
        // 최대 비용 체크
        if (prefs.getMaxCost() > 0 && route.getMetrics().get("cost") > prefs.getMaxCost()) {
            return false;
        }
        
        // 최대 소요시간 체크
        if (prefs.getMaxDuration() != null && 
            route.getTotalDuration() > prefs.getMaxDuration()) {
            return false;
        }
        
        return true;
    }

    private List<Location> generateTransitPoints(Location start, Location end) {
        List<Location> points = new ArrayList<>();
        
        // 시작점과 도착점 사이의 중간 지점들 계산
        double midLat = (start.getLatitude() + end.getLatitude()) / 2;
        double midLon = (start.getLongitude() + end.getLongitude()) / 2;
        
        // 기본 중간점
        points.add(new Location(midLat, midLon));
        
        // 약간 벗어난 지점들 (다양한 경로 옵션을 위해)
        points.add(new Location(midLat + 0.005, midLon + 0.005));
        points.add(new Location(midLat - 0.005, midLon - 0.005));
        points.add(new Location(midLat + 0.005, midLon - 0.005));
        points.add(new Location(midLat - 0.005, midLon + 0.005));
        
        return points;
    }

    private RoutePart createRoutePart(
            Location start,
            Location end,
            TransportMode mode,
            RouteDetails details) {
        return RoutePart.builder()
            .start(start)
            .end(end)
            .mode(mode)
            .distance(details.getTotalDistance())
            .duration(details.getTotalDuration())
            .cost(details.getMetrics().get("cost"))
            .instruction(generateInstruction(mode, start, end))
            .build();
    }

    private List<RouteOption> scoreAndFilterRoutes(
            List<RouteOption> routes,
            RoutePreferences prefs) {
        // 선호도에 따른 가중치 설정
        double timeWeight = prefs.isMinimizeTime() ? 0.5 : 0.3;
        double costWeight = prefs.isMinimizeCost() ? 0.5 : 0.3;
        double transferWeight = prefs.isMinimizeTransfers() ? 0.5 : 0.4;
        
        // 각 경로별 점수 계산
        for (RouteOption route : routes) {
            double timeScore = calculateTimeScore(route, routes);
            double costScore = calculateCostScore(route, routes);
            double transferScore = calculateTransferScore(route);
            
            double totalScore = (timeScore * timeWeight) +
                              (costScore * costWeight) +
                              (transferScore * transferWeight);
            
            route.setScore(totalScore);
        }
        
        // 점수 기반 정렬 및 상위 N개 선택
        return routes.stream()
            .sorted(Comparator.comparing(RouteOption::getScore).reversed())
            .limit(MAX_COMBINATIONS)
            .collect(Collectors.toList());
    }

    private double calculateTimeScore(RouteOption route, List<RouteOption> allRoutes) {
        int minDuration = allRoutes.stream()
            .mapToInt(RouteOption::getTotalDuration)
            .min()
            .orElse(route.getTotalDuration());
            
        return 1.0 - ((double) (route.getTotalDuration() - minDuration) / 
                      route.getTotalDuration());
    }

    private double calculateCostScore(RouteOption route, List<RouteOption> allRoutes) {
        double minCost = allRoutes.stream()
            .mapToDouble(RouteOption::getTotalCost)
            .min()
            .orElse(route.getTotalCost());
            
        return 1.0 - (route.getTotalCost() - minCost) / route.getTotalCost();
    }

    private double calculateTransferScore(RouteOption route) {
        return 1.0 - ((double) route.getNumberOfTransfers() / MAX_TRANSFERS);
    }

    private String generateInstruction(TransportMode mode, Location start, Location end) {
        String startName = start.getName() != null ? start.getName() : "출발지";
        String endName = end.getName() != null ? end.getName() : "도착지";
        
        return switch (mode) {
            case WALK -> String.format("%s에서 %s까지 도보로 이동", startName, endName);
            case BUS -> String.format("%s에서 %s까지 버스로 이동", startName, endName);
            case SUBWAY -> String.format("%s에서 %s까지 지하철로 이동", startName, endName);
            case TAXI -> String.format("%s에서 %s까지 택시로 이동", startName, endName);
        };
    }

    private String generateDescription(List<RoutePart> parts) {
        return parts.stream()
            .map(part -> String.format("%s (%.1fkm, %d분, %d원)",
                part.getMode().name(),
                part.getDistance(),
                part.getDuration(),
                (int) part.getCost()))
            .collect(Collectors.joining(" → "));
    }
}