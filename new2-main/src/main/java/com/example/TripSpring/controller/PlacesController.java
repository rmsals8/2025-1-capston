package com.example.TripSpring.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.example.TripSpring.dto.PlaceDetailsResponse;
import com.example.TripSpring.dto.PlacesResponse;
import com.example.TripSpring.service.PlacesService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlacesController {
    private final PlacesService placesService;

    @GetMapping("/search")
    public Mono<PlacesResponse> searchPlaces(
            @RequestParam String query,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {
        return placesService.searchPlaces(query, lat, lng);
    }

    @GetMapping("/details/{placeId}")
    public Mono<PlaceDetailsResponse> getPlaceDetails(@PathVariable String placeId) {
        return placesService.getPlaceDetails(placeId);
    }
}