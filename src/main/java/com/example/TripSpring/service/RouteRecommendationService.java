package com.example.TripSpring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.TripSpring.dto.domain.route.TransportMode;
import com.example.TripSpring.dto.request.route.RouteRecommendationRequest;
import com.example.TripSpring.dto.request.route.RouteRecommendationRequest.OptimizedSchedule;
import com.example.TripSpring.dto.response.RouteAnalysisResponse;
import com.example.TripSpring.dto.response.route.RoutePlaceDetailResponse;
import com.example.TripSpring.dto.response.route.RouteRecommendationResponse;
import com.example.TripSpring.dto.response.route.RouteSegmentDetailResponse;
import com.example.TripSpring.exception.RouteAnalysisException;
import com.example.TripSpring.service.analysis.PlaceDetailsAnalyzer;
import com.example.TripSpring.service.analysis.RouteMetricsAnalyzer;
import com.example.TripSpring.service.analysis.RouteSegmentAnalyzer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteRecommendationService {
    private final RouteSegmentAnalyzer segmentAnalyzer;
    private final PlaceDetailsAnalyzer placeAnalyzer;
    private final RouteMetricsAnalyzer metricsAnalyzer;

    private static final int MAX_REASONABLE_DURATION = 180; // 최대 3시간
    private static final int MIN_SEGMENT_DURATION = 10; // 최소 10분

    @Transactional(readOnly = true)
    @Cacheable(value = "routeRecommendations", key = "#request.hashCode()")
    public RouteRecommendationResponse generateRouteRecommendation(RouteRecommendationRequest request) {
        log.info("Generating route recommendation for {} locations", 
            request.getOptimizedSchedules().size());

        validateRequest(request);

        var response = new RouteRecommendationResponse();

        try {
            // 비동기로 각 분석 수행
            CompletableFuture<List<RouteSegmentDetailResponse>> segmentsFuture = 
                CompletableFuture.supplyAsync(() -> analyzeRouteSegments(request.getOptimizedSchedules()));

            CompletableFuture<List<RoutePlaceDetailResponse>> placesFuture = 
                CompletableFuture.supplyAsync(() -> analyzePlaceDetails(request.getOptimizedSchedules()));

            CompletableFuture<RouteAnalysisResponse> analysisFuture = 
                CompletableFuture.supplyAsync(() -> generateRouteAnalysis(request.getOptimizedSchedules()));

            // 모든 분석 결과 취합
            CompletableFuture.allOf(segmentsFuture, placesFuture, analysisFuture).join();

            response.setSegments(adjustSegmentDurations(segmentsFuture.get()));
            response.setPlaces(placesFuture.get());
            response.setAnalysis(adjustAnalysis(analysisFuture.get()));

            log.info("Route recommendation generated successfully");

        } catch (Exception e) {
            log.error("Error generating route recommendation: {}", e.getMessage());
            throw new RouteAnalysisException("Failed to generate route recommendation", e);
        }

        return response;
    }

    private List<RouteSegmentDetailResponse> adjustSegmentDurations(
            List<RouteSegmentDetailResponse> segments) {
        for (RouteSegmentDetailResponse segment : segments) {
            // 비현실적으로 긴 이동시간 조정
            if (segment.getDuration() > MAX_REASONABLE_DURATION) {
                int adjustedDuration = calculateReasonableDuration(
                    segment.getDistance(), 
                    segment.getTransportMode()
                );
                segment.setDuration(adjustedDuration);
            }
            // 너무 짧은 이동시간 조정
            if (segment.getDuration() < MIN_SEGMENT_DURATION) {
                segment.setDuration(MIN_SEGMENT_DURATION);
            }
        }
        return segments;
    }

    private RouteAnalysisResponse adjustAnalysis(RouteAnalysisResponse analysis) {
        // 전체 소요시간이 비현실적인 경우 조정
        if (analysis.getRouteMetrics().getTotalDuration() > MAX_REASONABLE_DURATION * 3) {
            analysis.getRouteMetrics().setTotalDuration(
                calculateReasonableTotalDuration(analysis.getRouteMetrics().getTotalDistance())
            );
        }
        return analysis;
    }

    private int calculateReasonableDuration(double distance, TransportMode mode) {
        // 이동 수단별 평균 속도 기반 계산 (km/h)
        double speed = switch (mode) {
            case WALK -> 4.0;     // 도보 평균 4km/h
            case BUS -> 20.0;     // 버스 평균 20km/h
            case SUBWAY -> 40.0;  // 지하철 평균 40km/h
            case TAXI -> 30.0;    // 택시 평균 30km/h
        };
        
        // 거리(km) / 속도(km/h) * 60 = 소요 시간(분)
        return (int) Math.ceil((distance / speed) * 60);
    }
    private int calculateReasonableTotalDuration(double totalDistance) {
        // 전체 거리에 대한 현실적인 소요시간 계산
        // 기본적으로 대중교통 속도로 계산
        double averageSpeed = 15.0; // km/h
        return (int) Math.ceil((totalDistance / averageSpeed) * 60);
    }

    private void validateRequest(RouteRecommendationRequest request) {
        if (request.getOptimizedSchedules() == null || 
            request.getOptimizedSchedules().isEmpty()) {
            throw new IllegalArgumentException("Schedules cannot be empty");
        }

        // 시간 순서 검증
        var schedules = request.getOptimizedSchedules();
        for (int i = 0; i < schedules.size() - 1; i++) {
            var current = schedules.get(i);
            var next = schedules.get(i + 1);

            LocalDateTime currentEnd = current.getEndTime();
            LocalDateTime nextStart = next.getStartTime();

            if (currentEnd.isAfter(nextStart)) {
                throw new IllegalArgumentException(
                    String.format("Invalid schedule order: %s ends after %s starts",
                        current.getName(), next.getName())
                );
            }

            Duration gap = Duration.between(currentEnd, nextStart);
            if (gap.toMinutes() > MAX_REASONABLE_DURATION) {
                log.warn("Large time gap ({} minutes) between {} and {}",
                    gap.toMinutes(), current.getName(), next.getName());
            }
        }
    }

    private List<RouteSegmentDetailResponse> analyzeRouteSegments(
            List<OptimizedSchedule> schedules) {
        return segmentAnalyzer.analyzeSegments(schedules);
    }

    private List<RoutePlaceDetailResponse> analyzePlaceDetails(
            List<OptimizedSchedule> schedules) {
        return placeAnalyzer.analyzeSchedules(schedules);
    }

    private RouteAnalysisResponse generateRouteAnalysis(
            List<OptimizedSchedule> schedules) {
        return metricsAnalyzer.analyzeRoute(schedules);
    }
}