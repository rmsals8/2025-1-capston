package com.example.TripSpring.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import com.example.TripSpring.dto.domain.Location;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.TripSpring.dto.request.FlexibleScheduleRequest;

import com.example.TripSpring.dto.response.FlexibleScheduleResponse;
import com.example.TripSpring.service.FlexibleScheduleService;
import com.example.TripSpring.service.ScheduleOptimizationService;
import com.example.TripSpring.dto.scheduler.OptimizeResponse;
import com.example.TripSpring.dto.scheduler.ScheduleOptimizationRequest;
import com.example.TripSpring.dto.domain.Schedule;
import java.util.List;
import com.example.TripSpring.dto.domain.ScheduleType;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class FlexibleScheduleController {
    
    private final FlexibleScheduleService flexibleScheduleService;
    private final ScheduleOptimizationService scheduleService;
    private final ObjectMapper objectMapper;
    
    @PostMapping("/optimize-1")
    public ResponseEntity<OptimizeResponse> optimizeSchedule(@RequestBody ScheduleOptimizationRequest request) {
        try {
            // 전체 요청 데이터 로깅
            try {
                log.info("Full request data: {}", objectMapper.writeValueAsString(request));
            } catch (Exception e) {
                log.warn("Failed to serialize request for logging", e);
            }
            
            log.info("Received schedule optimization request: {} fixed schedules, {} flexible schedules",
                request.getFixedSchedules().size(), 
                request.getFlexibleSchedules().size());
            
            // 유효성 검사 추가
            if (request.getFixedSchedules().isEmpty()) {
                throw new IllegalArgumentException("At least one fixed schedule is required");
            }
            
            // DTO를 도메인 객체로 변환
            List<Schedule> fixedSchedules = convertFixedSchedules(request.getFixedSchedules());
            List<Schedule> flexibleSchedules = convertFlexibleSchedules(request.getFlexibleSchedules());
            
            log.info("Converted to {} fixed and {} flexible domain schedules", 
                fixedSchedules.size(), flexibleSchedules.size());
            
            // 최적화 서비스 호출
            OptimizeResponse response = scheduleService.optimizeSchedule(fixedSchedules, flexibleSchedules);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request data: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error optimizing schedules", e);
            throw new RuntimeException("Failed to optimize schedules", e);
        }
    }
    
    private List<Schedule> convertFixedSchedules(List<ScheduleOptimizationRequest.FixedScheduleDTO> dtoList) {
        List<Schedule> schedules = new ArrayList<>();
        
        for (ScheduleOptimizationRequest.FixedScheduleDTO dto : dtoList) {
            // Location 객체 생성
            Location location = new Location(dto.getLatitude(), dto.getLongitude(), dto.getName());
            
            // Builder로 Schedule 생성
            Schedule schedule = Schedule.builder()
                .id(dto.getId())
                .name(dto.getName())
                .type(ScheduleType.FIXED)
                .location(location)
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .priority(dto.getPriority())
                .estimatedDuration(dto.getDuration())
                .build();
                
            // 문자열 위치 설정
            schedule.setLocationString(dto.getLocation());
            
            schedules.add(schedule);
        }
        
        return schedules;
    }
    
    private List<Schedule> convertFlexibleSchedules(List<ScheduleOptimizationRequest.FlexibleScheduleDTO> dtoList) {
        List<Schedule> schedules = new ArrayList<>();
        
        for (ScheduleOptimizationRequest.FlexibleScheduleDTO dto : dtoList) {
            // 기본 위치 객체 생성 (중요!)
            Location defaultLocation = new Location(0.0, 0.0, dto.getName());
            
            // Builder로 Schedule 생성
            Schedule schedule = Schedule.builder()
                .id(dto.getId())
                .name(dto.getName())
                .type(ScheduleType.FLEXIBLE)
                .location(defaultLocation) // 기본 위치 설정
                .priority(dto.getPriority())
                .estimatedDuration(dto.getDuration())
                .build();
                
            schedules.add(schedule);
        }
        
        return schedules;
    }
    
    @PostMapping("/optimize-flexible")
    public ResponseEntity<FlexibleScheduleResponse> optimizeFlexibleSchedules(
            @RequestBody FlexibleScheduleRequest request) {
        
        // 디버깅 정보 추가
        log.info("유연한 일정 최적화 요청 받음: 고정 일정 {}개, 유연한 일정 {}개",
                request.getFixedSchedules().size(), request.getFlexibleOptions().size());
        
        // 요청 데이터 검증
        if (request.getFixedSchedules().isEmpty()) {
            return ResponseEntity.badRequest().body(
                FlexibleScheduleResponse.builder()
                    .build()
            );
        }
        
        FlexibleScheduleResponse response = flexibleScheduleService.optimizeFlexibleSchedules(
                request.getFixedSchedules(),
                request.getFlexibleOptions()
        );
        
        log.info("생성된 경로 옵션: {}개", response.getRouteOptions().size());
        
        return ResponseEntity.ok(response);
    }
}