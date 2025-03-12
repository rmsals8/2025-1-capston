package com.example.TripSpring.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.example.TripSpring.dto.request.route.RouteRecommendationRequest;
import com.example.TripSpring.dto.response.route.RouteRecommendationResponse;
import com.example.TripSpring.service.RouteRecommendationService;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Slf4j
@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
@Tag(name = "Route Recommendation", description = "경로 추천 API")
public class RouteRecommendationController {
    private final RouteRecommendationService recommendationService;

    @Operation(summary = "경로 추천 생성", description = "최적화된 일정을 기반으로 상세 경로를 추천합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "경로 추천 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/recommended-path")
    public ResponseEntity<RouteRecommendationResponse> getRouteRecommendation(
            @RequestBody RouteRecommendationRequest request) {
        try {
            // 요청 로깅
            log.info("Received route recommendation request: {}", request);
            
            // 기본 검증
            validateRequest(request);

            RouteRecommendationResponse response = 
                recommendationService.generateRouteRecommendation(request);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 상세 로깅
            log.error("Route recommendation failed", e);
            throw new RuntimeException("Route recommendation failed: " + e.getMessage(), e);
        }
    }
    private void validateRequest(RouteRecommendationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Route recommendation request cannot be null");
        }
        
        if (request.getOptimizedSchedules() == null || request.getOptimizedSchedules().isEmpty()) {
            throw new IllegalArgumentException("Optimized schedules are required");
        }
    }
}