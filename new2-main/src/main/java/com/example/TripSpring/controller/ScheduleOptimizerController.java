package com.example.TripSpring.controller;

import com.example.TripSpring.service.ScheduleOptimizerService;
import com.example.TripSpring.service.RouteAnalyzerService;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/schedule")
@RequiredArgsConstructor
public class ScheduleOptimizerController {

    private final ScheduleOptimizerService optimizerService;

    @PostMapping("/optimize")
    public ResponseEntity<?> optimizeSchedule(@RequestBody OptimizationRequestDto request) {
        try {
            // DTO를 서비스 요청 객체로 변환
            ScheduleOptimizerService.OptimizationRequest serviceRequest = new ScheduleOptimizerService.OptimizationRequest(
                request.getPlaceName(),
                request.getLatitude(),
                request.getLongitude(),
                request.getEarliestStart(),
                request.getLatestEnd(),
                request.getExistingSchedules().stream()
                    .map(dto -> new ScheduleOptimizerService.ScheduleSlot(
                        dto.getPlaceName(),
                        dto.getLatitude(),
                        dto.getLongitude(),
                        dto.getStartTime(),
                        dto.getEndTime()
                    ))
                    .toList(),
                request.getDesiredDuration()
            );

            // 최적화 수행
            ScheduleOptimizerService.OptimizationResult result = optimizerService.findOptimalTimeSlot(serviceRequest);

            if (result == null) {
                return ResponseEntity.badRequest().body("No suitable time slot found");
            }

            // 결과를 DTO로 변환하여 반환
            OptimizationResultDto response = new OptimizationResultDto();
            response.setOptimalStartTime(result.getOptimalStartTime());
            response.setOptimalEndTime(result.getOptimalEndTime());
            response.setScore(result.getScore());
            response.setComponentScores(result.getComponentScores());
            response.setRouteSegments(result.getRouteSegments());
            response.setRecommendation(result.getRecommendation());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error optimizing schedule: " + e.getMessage());
        }
    }

    // Request DTO
    @Data
    public static class OptimizationRequestDto {
        private String placeName;
        private double latitude;
        private double longitude;
        private LocalDateTime earliestStart;
        private LocalDateTime latestEnd;
        private List<ScheduleSlotDto> existingSchedules;
        private int desiredDuration;
    }

    @Data
    public static class ScheduleSlotDto {
        private String placeName;
        private double latitude;
        private double longitude;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }

    // Response DTO
    @Data
    public static class OptimizationResultDto {
        private LocalDateTime optimalStartTime;
        private LocalDateTime optimalEndTime;
        private double score;
        private Map<String, Double> componentScores;
        private List<RouteAnalyzerService.RouteSegment> routeSegments;
        private String recommendation;
    }
}