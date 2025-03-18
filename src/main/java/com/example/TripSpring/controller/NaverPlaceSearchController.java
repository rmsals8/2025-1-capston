package com.example.TripSpring.controller;

import com.example.TripSpring.service.NaverPlaceSearchService;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/places")  // API 경로 추가
@RequiredArgsConstructor
// @CrossOrigin(origins = "*")   
@CrossOrigin(origins = "*", allowCredentials = "false")
public class NaverPlaceSearchController {
    private final NaverPlaceSearchService naverPlaceSearchService;

    // @GetMapping("/search-place")
    // public Map<String, Object> searchPlace(@RequestParam String query) {
    //     try {
    //         // 주 API 호출
    //         Map<String, Object> result = naverPlaceSearchService.searchPlaces(query);
            
    //         // 실패 시 대체 메서드 호출
    //         if (result.containsKey("status") && result.get("status").equals("ERROR")) {
    //             return naverPlaceSearchService.searchPlacesAlternative(query);
    //         }
            
    //         return result;
    //     } catch (Exception e) {
    //         return Map.of(
    //             "status", "ERROR", 
    //             "message", "검색 중 심각한 오류 발생: " + e.getMessage()
    //         );
    //     }
    // }
    @GetMapping("/search")         // 경로 변경
    public Map<String, Object> searchPlace(@RequestParam String query) {
        try {
            // 주 API 호출
            Map<String, Object> result = naverPlaceSearchService.searchPlaces(query);

            // 실패 시 대체 메서드 호출
            if (result.containsKey("status") && result.get("status").equals("ERROR")) {
                return naverPlaceSearchService.searchPlacesAlternative(query);
            }

            return result;
        } catch (Exception e) {
            return Map.of(
                "status", "ERROR", 
                "message", "검색 중 오류 발생: " + e.getMessage()
            );
        }
    }
}
