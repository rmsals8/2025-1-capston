package com.example.TripSpring.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.TripSpring.service.NearbyPlaceService;
import com.example.TripSpring.dto.response.NearbyPlacesResponse;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class NearbyPlaceController {
    
    private final NearbyPlaceService nearbyPlaceService;
    
    @GetMapping("/nearby")
    public ResponseEntity<NearbyPlacesResponse> findNearbyPlaces(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam String type,
            @RequestParam(defaultValue = "1000") double radius) {
        
        log.info("Searching for {} near lat: {}, lon: {} within {}m", type, lat, lon, radius);
        
        NearbyPlacesResponse response = nearbyPlaceService.findNearbyPlaces(lat, lon, type, radius);
        
        log.info("Found {} places", response.getPlaces().size());
        
        return ResponseEntity.ok(response);
    }
}
