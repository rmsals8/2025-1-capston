package com.example.TripSpring.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.TripSpring.service.NaverSearchService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/search")
public class SearchController {
    private final NaverSearchService naverSearchService;
    
    @Autowired
    public SearchController(NaverSearchService naverSearchService) {
        this.naverSearchService = naverSearchService;
    }
    
    @GetMapping("/place")
    public ResponseEntity<?> searchPlace(@RequestParam String query) {
        try {
            Map<String, Object> result = naverSearchService.searchPlaces(query);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to search places: " + e.getMessage()));
        }
    }
}

@Data 
@AllArgsConstructor 
class ErrorResponse {
    private String message;
}