// src/main/java/com/example/TripSpring/controller/VisitHistoryController.java
package com.example.TripSpring.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.TripSpring.domain.VisitHistory;
import com.example.TripSpring.dto.VisitHistoryDto;
import com.example.TripSpring.security.UserPrincipal;
import com.example.TripSpring.service.VisitHistoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/visit-histories")
@RequiredArgsConstructor
public class VisitHistoryController {
    
    private final VisitHistoryService visitHistoryService;
    
    @PostMapping("/add")
    public ResponseEntity<VisitHistory> addVisitHistory(
            @RequestBody VisitHistoryDto visitHistoryDto,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        VisitHistory addedHistory = visitHistoryService.addVisitHistory(
            visitHistoryDto, 
            userPrincipal.getId().toString()
        );
        
        return ResponseEntity.ok(addedHistory);
    }
    
    @GetMapping
    public ResponseEntity<List<VisitHistory>> getVisitHistories(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) String category) {
        
        String userId = userPrincipal.getId().toString();
        List<VisitHistory> histories;
        
        if (category != null && !category.isBlank()) {
            histories = visitHistoryService.getVisitHistoriesByCategory(userId, category);
        } else {
            histories = visitHistoryService.getVisitHistories(userId);
        }
        
        return ResponseEntity.ok(histories);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getCategoryStats(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        List<Object[]> stats = visitHistoryService.getCategoryStats(
            userPrincipal.getId().toString()
        );
        
        Map<String, Long> result = new HashMap<>();
        for (Object[] stat : stats) {
            String category = (String) stat[0];
            Long count = ((Number) stat[1]).longValue();
            result.put(category, count);
        }
        
        return ResponseEntity.ok(result);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVisitHistory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        visitHistoryService.deleteVisitHistory(id, userPrincipal.getId().toString());
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping
    public ResponseEntity<Void> deleteAllVisitHistories(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        visitHistoryService.deleteAllVisitHistories(userPrincipal.getId().toString());
        return ResponseEntity.ok().build();
    }
}