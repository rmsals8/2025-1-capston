package com.example.TripSpring.controller;

import com.example.TripSpring.domain.ScheduleData;
import com.example.TripSpring.dto.request.ScheduleInputRequest;
import com.example.TripSpring.dto.request.ScheduleOptimizationRequest;
import com.example.TripSpring.dto.response.ScheduleProcessResponse;
import com.example.TripSpring.service.ScheduleProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/schedule")
@RequiredArgsConstructor
public class ScheduleInputController {
    private final ScheduleProcessorService scheduleProcessorService;


    @PostMapping("/convert")
    public ResponseEntity<ScheduleOptimizationRequest> convertScheduleInput(
            @RequestBody String input) {
        log.info("Received schedule input: {}", input);
        
        ScheduleOptimizationRequest request = scheduleProcessorService.convertToScheduleRequest(input);
        return ResponseEntity.ok(request);
    }
    @PostMapping("/process")
    public ResponseEntity<ScheduleProcessResponse> processInput(
            @Validated @RequestBody ScheduleInputRequest request) {
        log.info("Received schedule processing request: {}", request);
        
        try {
            ScheduleData scheduleData = scheduleProcessorService.processInput(
                request.getInput(),
                request.getType()
            );
            
            ScheduleProcessResponse response = ScheduleProcessResponse.builder()
                .location(scheduleData.getLocation())
                .time(scheduleData.getTime())
                .priority(scheduleData.getPriority())
                .type(scheduleData.getType().toString())
                .build();
                
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing schedule input", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}