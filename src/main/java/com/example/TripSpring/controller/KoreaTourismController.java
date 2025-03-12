package com.example.TripSpring.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.TripSpring.service.KoreaTourismService;

@Slf4j
@RestController
@RequestMapping("/api/tourism")
@RequiredArgsConstructor
public class KoreaTourismController {
   private final KoreaTourismService koreaTourismService;

   @GetMapping("/search")
   public ResponseEntity<String> searchTouristSpots(
           @RequestParam String keyword,
           @RequestParam(defaultValue = "1") int pageNo,
           @RequestParam(defaultValue = "10") int numOfRows) {
       try {
           String result = koreaTourismService.searchTouristSpots(keyword, pageNo, numOfRows);
           log.info("API Response: {}", result);
           return ResponseEntity.ok(result);
       } catch (Exception e) {
           log.error("Error occurred while searching tourist spots: ", e);
           return ResponseEntity.internalServerError().body(e.getMessage());
       }
   }

   @GetMapping("/details")
   public ResponseEntity<String> getSpotDetails(
           @RequestParam String contentId,
           @RequestParam String contentTypeId) {
       try {
           String result = koreaTourismService.getSpotDetails(contentId, contentTypeId);
           log.info("API Response for details: {}", result);
           return ResponseEntity.ok(result);
       } catch (Exception e) {
           log.error("Error occurred while getting spot details: ", e);
           return ResponseEntity.internalServerError().body(e.getMessage());
       }
   }

   @GetMapping("/area")
   public ResponseEntity<String> getAreaBasedList(
           @RequestParam String areaCode,
           @RequestParam(required = false) String sigunguCode) {
       try {
           String result = koreaTourismService.getAreaBasedList(areaCode, sigunguCode);
           log.info("API Response for area: {}", result);
           return ResponseEntity.ok(result);
       } catch (Exception e) {
           log.error("Error occurred while getting area based list: ", e);
           return ResponseEntity.internalServerError().body(e.getMessage());
       }
   }
}