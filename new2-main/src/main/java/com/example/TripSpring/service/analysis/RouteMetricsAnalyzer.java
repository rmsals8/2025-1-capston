package com.example.TripSpring.service.analysis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.example.TripSpring.dto.request.route.RouteRecommendationRequest.OptimizedSchedule;
import com.example.TripSpring.dto.response.RouteAnalysisResponse;
import com.example.TripSpring.service.FirstMapService;
import com.example.TripSpring.service.calculator.CrowdLevelAnalyzer;
import com.example.TripSpring.dto.domain.TrafficInfo;
import com.example.TripSpring.dto.domain.Location;
import com.example.TripSpring.dto.domain.route.TransportMode;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class RouteMetricsAnalyzer {
    private final FirstMapService firstMapService;
    private final CrowdLevelAnalyzer crowdLevelAnalyzer;
    
    private static final double EASY_DISTANCE_THRESHOLD = 5.0;
    private static final double MODERATE_DISTANCE_THRESHOLD = 10.0;
    private static final int EASY_DURATION_THRESHOLD = 240;
    private static final int MODERATE_DURATION_THRESHOLD = 480;

    public RouteAnalysisResponse analyzeRoute(List<OptimizedSchedule> schedules) {
        try {
            // 기본 메트릭스 계산
            RouteAnalysisResponse.RouteMetrics metrics = calculateBaseMetrics(schedules);
            
            // 세그먼트 분석
            List<RouteAnalysisResponse.SegmentAnalysis> segments = analyzeSegments(schedules);
            
            // 시간대별 분석
            List<RouteAnalysisResponse.TimeSlotAnalysis> timeSlots = analyzeTimeSlots(schedules);
            
            // 권장사항 생성
            List<String> recommendations = generateRecommendations(schedules, metrics);

            return RouteAnalysisResponse.builder()
                .routeMetrics(metrics)
                .segments(segments)
                .timeSlots(timeSlots)
                .recommendations(recommendations)
                .categoryDistribution(calculateCategoryDistribution(schedules))
                .optimization(generateOptimizationDetails(metrics))
                .build();

        } catch (Exception e) {
            log.error("Error analyzing route: {}", e.getMessage());
            return createDefaultResponse();
        }
    }

    private RouteAnalysisResponse.RouteMetrics calculateBaseMetrics(List<OptimizedSchedule> schedules) {
        double totalDistance = 0.0;
        int totalDuration = 0;
        Map<String, Integer> modeSplit = new HashMap<>();

        for (int i = 0; i < schedules.size() - 1; i++) {
            OptimizedSchedule current = schedules.get(i);
            OptimizedSchedule next = schedules.get(i + 1);

            TrafficInfo trafficInfo = firstMapService.getTrafficInfo(
                new Location(current.getLocation().getLatitude(), current.getLocation().getLongitude()),
                new Location(next.getLocation().getLatitude(), next.getLocation().getLongitude())
            );

            totalDistance += trafficInfo.getDistance();
            TransportMode mode = determineTransportMode(trafficInfo.getDistance(), current.getStartTime());
            int duration = calculateTravelTime(trafficInfo.getDistance(), current.getStartTime(), mode);
            totalDuration += duration;

            // 이동수단 통계
            modeSplit.merge(mode.name(), 1, Integer::sum);
        }

        String difficulty = assessDifficulty(totalDistance, totalDuration, schedules);

        return RouteAnalysisResponse.RouteMetrics.builder()
            .totalDistance(totalDistance)
            .totalDuration(totalDuration)
            .difficulty(difficulty)
            .averageSpeed(calculateAverageSpeed(totalDistance, totalDuration))
            .totalStops(schedules.size())
            .modeSplit(modeSplit)
            .build();
    }

    private List<RouteAnalysisResponse.SegmentAnalysis> analyzeSegments(List<OptimizedSchedule> schedules) {
        List<RouteAnalysisResponse.SegmentAnalysis> segments = new ArrayList<>();
        
        for (int i = 0; i < schedules.size() - 1; i++) {
            OptimizedSchedule current = schedules.get(i);
            OptimizedSchedule next = schedules.get(i + 1);
            
            TrafficInfo trafficInfo = firstMapService.getTrafficInfo(
                new Location(current.getLocation().getLatitude(), current.getLocation().getLongitude()),
                new Location(next.getLocation().getLatitude(), next.getLocation().getLongitude())
            );

            TransportMode mode = determineTransportMode(trafficInfo.getDistance(), current.getStartTime());
            
            segments.add(RouteAnalysisResponse.SegmentAnalysis.builder()
                .fromLocation(current.getName())
                .toLocation(next.getName())
                .distance(trafficInfo.getDistance())
                .duration(calculateTravelTime(trafficInfo.getDistance(), current.getStartTime(), mode))
                .transportMode(mode.name())
                .crowdLevel(crowdLevelAnalyzer.analyzeCrowdLevel(current.getStartTime()))
                .alerts(generateAlerts(current.getStartTime(), mode))
                .build());
        }
        
        return segments;
    }

    private List<RouteAnalysisResponse.TimeSlotAnalysis> analyzeTimeSlots(List<OptimizedSchedule> schedules) {
        return schedules.stream()
            .map(schedule -> RouteAnalysisResponse.TimeSlotAnalysis.builder()
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .crowdedness(crowdLevelAnalyzer.analyzeCrowdLevel(schedule.getStartTime()))
                .trafficCondition(determineTrafficCondition(schedule.getStartTime()))
                .isRushHour(isRushHour(schedule.getStartTime()))
                .isOptimalTime(!isRushHour(schedule.getStartTime()))
                .considerations(generateConsiderations(schedule))
                .build())
            .toList();
    }

    private List<String> generateRecommendations(List<OptimizedSchedule> schedules, 
            RouteAnalysisResponse.RouteMetrics metrics) {
        List<String> recommendations = new ArrayList<>();
        
        if (metrics.getTotalDuration() > MODERATE_DURATION_THRESHOLD) {
            recommendations.add("일정이 다소 길어 중간에 충분한 휴식을 취하는 것을 추천합니다.");
        }

        for (OptimizedSchedule schedule : schedules) {
            if (isLunchTime(schedule.getStartTime())) {
                recommendations.add(String.format("%s 방문이 점심 시간대와 겹치므로 식사 계획을 미리 세우는 것을 추천합니다.", 
                    schedule.getName()));
            }
            if (isRushHour(schedule.getStartTime())) {
                recommendations.add(String.format("%s 방문이 혼잡 시간대와 겹치므로 여유있게 이동하시기 바랍니다.", 
                    schedule.getName()));
            }
        }

        return recommendations;
    }

    private Map<String, Double> calculateCategoryDistribution(List<OptimizedSchedule> schedules) {
        Map<String, Double> distribution = new HashMap<>();
        int total = schedules.size();
        
        for (OptimizedSchedule schedule : schedules) {
            String category = determineCategory(schedule.getName());
            distribution.merge(category, 1.0/total * 100, Double::sum);
        }
        
        return distribution;
    }

    private RouteAnalysisResponse.OptimizationDetails generateOptimizationDetails(
            RouteAnalysisResponse.RouteMetrics metrics) {
        return RouteAnalysisResponse.OptimizationDetails.builder()
            .iterationCount(1)
            .originalDuration(metrics.getTotalDuration() * 1.2)  // 예시값
            .optimizedDuration(metrics.getTotalDuration())
            .improvementPercentage(20.0)  // 예시값
            .appliedStrategies(List.of("시간대별 최적화", "교통수단 최적화"))
            .build();
    }

    // 기존의 유틸리티 메서드들...
    private TransportMode determineTransportMode(double distance, LocalDateTime time) {
        if (distance <= 0.8) return TransportMode.WALK;
        if (distance <= 3.0) return isRushHour(time) ? TransportMode.SUBWAY : TransportMode.BUS;
        return TransportMode.TAXI;
    }

    private int calculateTravelTime(double distance, LocalDateTime time, TransportMode mode) {
        double speedKmH = switch (mode) {
            case WALK -> 4.0;
            case BUS -> 20.0;
            case SUBWAY -> 40.0;
            case TAXI -> 30.0;
        };
        
        return (int) (distance / speedKmH * 60);
    }

    private boolean isRushHour(LocalDateTime time) {
        int hour = time.getHour();
        return (hour >= 8 && hour <= 10) || (hour >= 17 && hour <= 19);
    }

    private boolean isLunchTime(LocalDateTime time) {
        int hour = time.getHour();
        return hour >= 11 && hour <= 13;
    }

    private String determineTrafficCondition(LocalDateTime time) {
        return isRushHour(time) ? "혼잡" : "원활";
    }

    private List<String> generateConsiderations(OptimizedSchedule schedule) {
        List<String> considerations = new ArrayList<>();
        if (isRushHour(schedule.getStartTime())) {
            considerations.add("혼잡 시간대이므로 여유있게 이동하세요.");
        }
        if (isLunchTime(schedule.getStartTime())) {
            considerations.add("점심 시간대입니다. 주변 식당이 혼잡할 수 있습니다.");
        }
        return considerations;
    }

    private List<String> generateAlerts(LocalDateTime time, TransportMode mode) {
        List<String> alerts = new ArrayList<>();
        if (isRushHour(time)) {
            alerts.add("혼잡 시간대 이동");
        }
        if (mode.isPublicTransit()) {
            alerts.add("대중교통 이용 구간");
        }
        return alerts;
    }

    private String assessDifficulty(double distance, int duration, List<OptimizedSchedule> schedules) {
        if (distance <= EASY_DISTANCE_THRESHOLD && duration <= EASY_DURATION_THRESHOLD) return "EASY";
        if (distance <= MODERATE_DISTANCE_THRESHOLD && duration <= MODERATE_DURATION_THRESHOLD) return "MODERATE";
        return "HARD";
    }

    private double calculateAverageSpeed(double totalDistance, int totalDuration) {
        return totalDuration > 0 ? (totalDistance / totalDuration) * 60 : 0;
    }

    private String determineCategory(String name) {
        if (name.contains("궁") || name.contains("박물관")) return "문화/역사";
        if (name.contains("시장") || name.contains("상가")) return "쇼핑";
        if (name.contains("공원") || name.contains("광장")) return "휴식";
        return "기타";
    }

    private RouteAnalysisResponse createDefaultResponse() {
        return RouteAnalysisResponse.builder()
            .routeMetrics(RouteAnalysisResponse.RouteMetrics.builder()
                .totalDistance(0.0)
                .totalDuration(0)
                .difficulty("UNKNOWN")
                .build())
            .segments(new ArrayList<>())
            .timeSlots(new ArrayList<>())
            .recommendations(List.of("경로 분석 중 오류가 발생했습니다."))
            .build();
    }
}