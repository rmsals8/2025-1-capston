package com.example.TripSpring.controller;

import com.example.TripSpring.dto.scheduler.OptimizeRequest;
import com.example.TripSpring.dto.scheduler.OptimizeResponse;
import com.example.TripSpring.service.ScheduleOptimizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleController {
    
    private final ScheduleOptimizationService optimizationService;
    
    @PostMapping("/optimize")
    public ResponseEntity<OptimizeResponse> optimizeSchedule(@RequestBody OptimizeRequest request) {
        try {
            log.info("Received schedule optimization request with {} fixed and {} flexible schedules",
                request.getFixedSchedules().size(),
                request.getFlexibleSchedules().size()
            );
            
            OptimizeResponse response = optimizationService.optimizeSchedule(
                request.getFixedSchedulesAsDomain(),
                request.getFlexibleSchedulesAsDomain()
            );
            
            log.info("Schedule optimization completed successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to optimize schedule", e);
            throw new RuntimeException("Schedule optimization failed", e);
        }
    }
    
    @PostMapping("/validate")
    public ResponseEntity<ValidationResponse> validateSchedule(@RequestBody OptimizeResponse optimizedSchedule) {
        try {
            log.info("Validating optimized schedule with {} schedules",
                optimizedSchedule.getOptimizedSchedules().size()
            );
            
            ValidationResponse response = new ValidationResponse();
            response.setValid(true);
            response.setMessage("Schedule validation successful");
            
            // 향후 검증 로직 추가 가능
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to validate schedule", e);
            ValidationResponse response = new ValidationResponse();
            response.setValid(false);
            response.setMessage("Schedule validation failed: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
    
    @Data
    private static class ValidationResponse {
        private boolean valid;
        private String message;
    }
}