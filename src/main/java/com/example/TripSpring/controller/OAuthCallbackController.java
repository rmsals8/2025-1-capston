package com.example.TripSpring.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.TripSpring.domain.user.User;
import com.example.TripSpring.security.JwtTokenProvider;
import com.example.TripSpring.service.oauth.KakaoOAuth2Service;
import com.example.TripSpring.service.oauth.NaverOAuth2Service;

@RestController
@RequestMapping("/api/oauth2/callback")
public class OAuthCallbackController {

    private final KakaoOAuth2Service kakaoOAuth2Service;
    private final JwtTokenProvider jwtTokenProvider;
    private final NaverOAuth2Service naverOAuth2Service; // 추가
    @Autowired
    public OAuthCallbackController(
            KakaoOAuth2Service kakaoOAuth2Service, 
            NaverOAuth2Service naverOAuth2Service, // 추가
            JwtTokenProvider jwtTokenProvider) {
        this.kakaoOAuth2Service = kakaoOAuth2Service;
        this.naverOAuth2Service = naverOAuth2Service; // 추가
        this.jwtTokenProvider = jwtTokenProvider;
    }
    @PostMapping("/kakao/token")
    public ResponseEntity<?> getKakaoToken(@RequestParam String code) {
        try {
            // 카카오 액세스 토큰 요청
            String accessToken = kakaoOAuth2Service.getAccessToken(code);
            
            // 카카오 사용자 정보 및 회원가입/로그인 처리
            User user = kakaoOAuth2Service.getOrCreateUser(accessToken);
            
            // 소셜 로그인용 JWT 토큰 생성
            String jwtToken = jwtTokenProvider.createSocialLoginToken(user.getEmail());
            String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());
            
            Map<String, String> response = new HashMap<>();
            response.put("accessToken", jwtToken);
            response.put("refreshToken", refreshToken);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/kakao")
    public ResponseEntity<?> kakaoCallback(@RequestParam String code) {
        try {
            // 카카오 액세스 토큰 요청
            String accessToken = kakaoOAuth2Service.getAccessToken(code);
            
            // 카카오 사용자 정보 및 회원가입/로그인 처리
            User user = kakaoOAuth2Service.getOrCreateUser(accessToken);
            
            // 소셜 로그인용 JWT 토큰 생성
            String jwtToken = jwtTokenProvider.createSocialLoginToken(user.getEmail());
            String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());
            
            // HTML 응답 생성 (앱으로 리다이렉트하기 위한 스크립트 포함)
            String htmlResponse = String.format(
                "<!DOCTYPE html>" +
                "<html>" +
                "<head><title>로그인 성공</title></head>" +
                "<body>" +
                "<h1>로그인 성공!</h1>" +
                "<p>앱으로 돌아가는 중...</p>" +
                "<script>" +
                "  window.onload = function() {" +
                "    window.location.href = 'tripapp://oauth2/redirect?token=%s&refresh_token=%s';" +
                "    setTimeout(function() {" +
                "      window.close();" +
                "    }, 1000);" +
                "  };" +
                "</script>" +
                "</body>" +
                "</html>",
                jwtToken, refreshToken);
            
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(htmlResponse);
                
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("로그인 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 네이버 토큰 엔드포인트 추가
    @PostMapping("/naver/token")
    public ResponseEntity<?> getNaverToken(@RequestParam String code, @RequestParam String state) {
        try {
            // 네이버 액세스 토큰 요청
            String accessToken = naverOAuth2Service.getAccessToken(code, state);
            
            // 네이버 사용자 정보 및 회원가입/로그인 처리
            User user = naverOAuth2Service.getOrCreateUser(accessToken);
            
            // 소셜 로그인용 JWT 토큰 생성
            String jwtToken = jwtTokenProvider.createSocialLoginToken(user.getEmail());
            String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());
            
            Map<String, String> response = new HashMap<>();
            response.put("accessToken", jwtToken);
            response.put("refreshToken", refreshToken);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    // 네이버 콜백 엔드포인트 추가
    @GetMapping("/naver")
    public ResponseEntity<?> naverCallback(@RequestParam String code, @RequestParam String state) {
        try {
            // 네이버 액세스 토큰 요청
            String accessToken = naverOAuth2Service.getAccessToken(code, state);
            
            // 네이버 사용자 정보 및 회원가입/로그인 처리
            User user = naverOAuth2Service.getOrCreateUser(accessToken);
            
            // 소셜 로그인용 JWT 토큰 생성
            String jwtToken = jwtTokenProvider.createSocialLoginToken(user.getEmail());
            String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());
            
            // HTML 응답 생성 (앱으로 리다이렉트하기 위한 스크립트 포함)
            String htmlResponse = String.format(
                "<!DOCTYPE html>" +
                "<html>" +
                "<head><title>로그인 성공</title></head>" +
                "<body>" +
                "<h1>로그인 성공!</h1>" +
                "<p>앱으로 돌아가는 중...</p>" +
                "<script>" +
                "  window.onload = function() {" +
                "    window.location.href = 'tripapp://oauth2/redirect?token=%s&refresh_token=%s';" +
                "    setTimeout(function() {" +
                "      window.close();" +
                "    }, 1000);" +
                "  };" +
                "</script>" +
                "</body>" +
                "</html>",
                jwtToken, refreshToken);
            
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(htmlResponse);
                
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("로그인 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
