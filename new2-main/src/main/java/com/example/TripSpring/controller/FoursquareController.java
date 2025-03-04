package com.example.TripSpring.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import com.example.TripSpring.dto.foursquare.FoursquarePlaceDetails;
import com.example.TripSpring.dto.foursquare.FoursquareResponse;
import com.example.TripSpring.service.FoursquareService;

@Slf4j
@RestController
@RequestMapping("/api/foursquare")
public class FoursquareController {
   private final FoursquareService foursquareService;

   @Autowired
   public FoursquareController(FoursquareService foursquareService) {
       this.foursquareService = foursquareService;
   }

   @GetMapping("/places/search")
   public ResponseEntity<FoursquareResponse> searchPlaces(
           @RequestParam String query,
           @RequestParam double lat,
           @RequestParam double lng,
           @RequestParam(defaultValue = "1000") int radius) {
       return ResponseEntity.ok(foursquareService.searchPlaces(query, lat, lng, radius));
   }

   @GetMapping("/places/{placeId}")
   public ResponseEntity<FoursquarePlaceDetails> getPlaceDetails(
           @PathVariable String placeId) {
       return ResponseEntity.ok(foursquareService.getPlaceDetails(placeId));
   }
}