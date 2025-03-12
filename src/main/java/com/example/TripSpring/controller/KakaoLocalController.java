package com.example.TripSpring.controller;

import com.example.TripSpring.service.KakaoLocalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class KakaoLocalController {
    private final KakaoLocalService kakaoLocalService;

    @GetMapping("/kakao-search-place")  // 엔드포인트 변경
    public Map<String, Object> searchPlace(@RequestParam String query) {
        return kakaoLocalService.searchPlace(query);
    }
}