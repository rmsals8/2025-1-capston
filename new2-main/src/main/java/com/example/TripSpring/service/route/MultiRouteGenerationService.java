//src/main/java/com/example/TripSpring/service/route/MultiRouteGenerationService.java
package com.example.TripSpring.service.route;

import com.example.TripSpring.dto.domain.Location;
import com.example.TripSpring.dto.route.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiRouteGenerationService {
    private final MultiModalRouteService multiModalRouteService;
    private final RouteSequenceService sequenceService;

    private static final int MAX_ROUTES = 3;  // 최대 추천 경로 수

    public List<CompleteRoute> generateMultipleRoutes(
            List<Location> waypoints,
            RoutePreferences prefs) {
        try {
            // 1. 경유지 순서 최적화
            List<List<Location>> optimizedSequences = generateOptimizedSequences(waypoints);
            
            // 2. 각 순서별로 경로 생성
            List<CompleteRoute> routes = new ArrayList<>();
            for (List<Location> sequence : optimizedSequences) {
                CompleteRoute route = generateCompleteRoute(sequence, prefs);
                routes.add(route);
            }
            
            // 3. 경로 평가 및 필터링
            return evaluateAndFilterRoutes(routes);
            
        } catch (Exception e) {
            log.error("Error generating multiple routes: {}", e.getMessage());
            throw new RuntimeException("Failed to generate routes", e);
        }
    }

    private List<List<Location>> generateOptimizedSequences(List<Location> waypoints) {
        List<List<Location>> sequences = new ArrayList<>();
        
        // 시작점과 끝점은 고정
        Location start = waypoints.get(0);
        Location end = waypoints.get(waypoints.size() - 1);
        List<Location> middlePoints = waypoints.subList(1, waypoints.size() - 1);

        // 중간 지점들의 순열 생성
        List<List<Location>> permutations = generatePermutations(middlePoints);
        
        // 각 순열에 시작점과 끝점 추가
        for (List<Location> permutation : permutations) {
            List<Location> sequence = new ArrayList<>();
            sequence.add(start);
            sequence.addAll(permutation);
            sequence.add(end);
            sequences.add(sequence);
        }

        return sequences;
    }

    private List<List<Location>> generatePermutations(List<Location> points) {
        List<List<Location>> result = new ArrayList<>();
        generatePermutationsHelper(points, 0, result);
        return result;
    }

    private void generatePermutationsHelper(
            List<Location> points,
            int start,
            List<List<Location>> result) {
        if (start == points.size()) {
            result.add(new ArrayList<>(points));
            return;
        }

        for (int i = start; i < points.size(); i++) {
            Collections.swap(points, start, i);
            generatePermutationsHelper(points, start + 1, result);
            Collections.swap(points, start, i);
        }
    }

    private CompleteRoute generateCompleteRoute(
            List<Location> sequence,
            RoutePreferences prefs) {
        List<RouteOption> options = new ArrayList<>();
        Map<String, Object> metadata = new HashMap<>();
        
        // 각 연속된 두 지점 간의 경로 옵션 계산
        for (int i = 0; i < sequence.size() - 1; i++) {
            Location current = sequence.get(i);
            Location next = sequence.get(i + 1);
            
            List<RouteOption> segmentOptions = 
                multiModalRouteService.calculateMultiModalRoutes(current, next, prefs);
            options.addAll(segmentOptions);
            
            // 구간별 메타데이터 수집
            collectSegmentMetadata(metadata, segmentOptions, i);
        }

        // 경로 특성 분석
        List<String> highlights = analyzeRouteHighlights(options);
        String recommendationType = determineRecommendationType(options, prefs);
        double score = calculateRouteScore(options, prefs);

        return CompleteRoute.builder()
            .routeId(UUID.randomUUID().toString())
            .options(options)
            .metadata(metadata)
            .recommendationType(recommendationType)
            .score(score)
            .highlights(highlights)
            .build();
    }

    private void collectSegmentMetadata(
            Map<String, Object> metadata,
            List<RouteOption> options,
            int segmentIndex) {
        // 구간별 통계 수집
        double avgDuration = options.stream()
            .mapToInt(RouteOption::getTotalDuration)
            .average()
            .orElse(0.0);
            
        double avgCost = options.stream()
            .mapToDouble(RouteOption::getTotalCost)
            .average()
            .orElse(0.0);
            
        metadata.put("segment_" + segmentIndex + "_avg_duration", avgDuration);
        metadata.put("segment_" + segmentIndex + "_avg_cost", avgCost);
    }

    private List<String> analyzeRouteHighlights(List<RouteOption> options) {
        List<String> highlights = new ArrayList<>();
        
        // 전체 경로 특성 분석
        double totalCost = options.stream()
            .mapToDouble(RouteOption::getTotalCost)
            .sum();
            
        int totalDuration = options.stream()
            .mapToInt(RouteOption::getTotalDuration)
            .sum();
            
        int totalTransfers = options.stream()
            .mapToInt(RouteOption::getNumberOfTransfers)
            .sum();

        // 주요 특징 추출
        if (totalTransfers == 0) {
            highlights.add("환승 없는 편한 경로");
        }
        if (totalCost < 5000) {  // 예시 기준
            highlights.add("경제적인 경로");
        }
        if (totalDuration < 60) {  // 예시 기준
            highlights.add("빠른 경로");
        }

        return highlights;
    }

    private String determineRecommendationType(
            List<RouteOption> options,
            RoutePreferences prefs) {
        if (prefs.isMinimizeTime()) {
            return "FASTEST";
        } else if (prefs.isMinimizeCost()) {
            return "CHEAPEST";
        } else if (prefs.isMinimizeTransfers()) {
            return "CONVENIENT";
        }
        return "BALANCED";
    }

    private double calculateRouteScore(
            List<RouteOption> options,
            RoutePreferences prefs) {
        // 기본 점수 계산 요소
        double timeScore = calculateTimeScore(options);
        double costScore = calculateCostScore(options);
        double transferScore = calculateTransferScore(options);
        
        // 선호도에 따른 가중치 적용
        double timeWeight = prefs.isMinimizeTime() ? 0.5 : 0.3;
        double costWeight = prefs.isMinimizeCost() ? 0.5 : 0.3;
        double transferWeight = prefs.isMinimizeTransfers() ? 0.5 : 0.4;
        
        return (timeScore * timeWeight) + 
               (costScore * costWeight) + 
               (transferScore * transferWeight);
    }

    private double calculateTimeScore(List<RouteOption> options) {
        int totalDuration = options.stream()
            .mapToInt(RouteOption::getTotalDuration)
            .sum();
        return 1.0 / (1.0 + totalDuration / 60.0);  // 1시간 기준 정규화
    }

    private double calculateCostScore(List<RouteOption> options) {
        double totalCost = options.stream()
            .mapToDouble(RouteOption::getTotalCost)
            .sum();
        return 1.0 / (1.0 + totalCost / 10000.0);  // 10000원 기준 정규화
    }

    private double calculateTransferScore(List<RouteOption> options) {
        int totalTransfers = options.stream()
            .mapToInt(RouteOption::getNumberOfTransfers)
            .sum();
        return 1.0 / (1.0 + totalTransfers);
    }

    private List<CompleteRoute> evaluateAndFilterRoutes(List<CompleteRoute> routes) {
        // 점수 기반 정렬
        routes.sort(Comparator.comparing(CompleteRoute::getScore).reversed());
        
        // 상위 N개 경로만 선택
        return routes.stream()
            .limit(MAX_ROUTES)
            .collect(Collectors.toList());
    }
}