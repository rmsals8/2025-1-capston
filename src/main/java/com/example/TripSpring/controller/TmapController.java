package com.example.TripSpring.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.example.TripSpring.dto.request.RouteRequest;

import com.example.TripSpring.service.TmapService;

@RestController
@RequestMapping("/api/tmap")
@RequiredArgsConstructor
@Slf4j
public class TmapController {
    private final TmapService tmapService;
    
    @GetMapping("/route")
    public ResponseEntity<String> getRoute(
            @RequestParam(required = true) Double startLat,
            @RequestParam(required = true) Double startLon,
            @RequestParam(required = true) Double endLat,
            @RequestParam(required = true) Double endLon) {
        try {
            validateCoordinates(startLat, startLon, endLat, endLon);
            
            log.info("Getting route from ({}, {}) to ({}, {})", 
                startLat, startLon, endLat, endLon);
            
            String result = tmapService.getDrivingRoute(startLat, startLon, endLat, endLon);
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid coordinates provided: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid coordinates: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to get route", e);
            return ResponseEntity.internalServerError()
                .body("Failed to get route: " + e.getMessage());
        }
    }

    @PostMapping("/route")
    public ResponseEntity<String> getRoutePost(@RequestBody @Validated RouteRequest request) {
        try {
            validateCoordinates(
                request.getStartLat(), 
                request.getStartLon(), 
                request.getEndLat(), 
                request.getEndLon()
            );
            
            log.info("Getting route from ({}, {}) to ({}, {})", 
                request.getStartLat(), request.getStartLon(), 
                request.getEndLat(), request.getEndLon());
            
            String result = tmapService.getDrivingRoute(
                request.getStartLat(),
                request.getStartLon(),
                request.getEndLat(),
                request.getEndLon()
            );
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid coordinates provided: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid coordinates: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to get route", e);
            return ResponseEntity.internalServerError()
                .body("Failed to get route: " + e.getMessage());
        }
    }

    @GetMapping("/traffic")
    public ResponseEntity<String> getTrafficInfo(
            @RequestParam(required = true) Double lat,
            @RequestParam(required = true) Double lon) {
        try {
            validateCoordinates(lat, lon);
            
            log.info("Getting traffic info for location ({}, {})", lat, lon);
            
            String result = tmapService.getTrafficInfo(lat, lon);
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid coordinates provided: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid coordinates: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to get traffic info", e);
            return ResponseEntity.internalServerError()
                .body("Failed to get traffic information: " + e.getMessage());
        }
    }
    private void validateCoordinates(Double lat, Double lon) {
        if (lat == null || lon == null) {
            throw new IllegalArgumentException("Latitude and longitude must not be null");
        }
        if (lat < -90 || lat > 90) {
            throw new IllegalArgumentException("Invalid latitude value: " + lat);
        }
        if (lon < -180 || lon > 180) {
            throw new IllegalArgumentException("Invalid longitude value: " + lon);
        }
    }

    private void validateCoordinates(Double startLat, Double startLon, Double endLat, Double endLon) {
        validateCoordinates(startLat, startLon);
        validateCoordinates(endLat, endLon);
    }
}
