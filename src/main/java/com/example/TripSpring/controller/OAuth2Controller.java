package com.example.TripSpring.controller;

import com.example.TripSpring.common.response.ApiResponse;
import com.example.TripSpring.domain.user.User;
import com.example.TripSpring.payload.response.AuthResponse;
import com.example.TripSpring.payload.response.UserProfile;
import com.example.TripSpring.security.JwtTokenProvider;
import com.example.TripSpring.service.oauth.KakaoOAuth2Service;
import com.example.TripSpring.service.oauth.NaverOAuth2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/v1/oauth2")
@RequiredArgsConstructor
@Tag(name = "OAuth2 인증", description = "소셜 로그인 관련 API")
public class OAuth2Controller {

    private final KakaoOAuth2Service kakaoService;
    private final NaverOAuth2Service naverService;
    private final JwtTokenProvider tokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @GetMapping("/callback/kakao")
    public ApiResponse<AuthResponse> kakaoCallback(@RequestParam String code) {
        try {
            log.info("Kakao OAuth2 callback received with code: {}", code);
            
            // 1. 카카오 액세스 토큰 획득
            String accessToken = kakaoService.getAccessToken(code);
            log.info("Kakao access token obtained: {}", accessToken);
            
            // 2. 사용자 정보 조회 또는 생성
            User user = kakaoService.getOrCreateUser(accessToken);
            log.info("User info obtained/created: {}", user);
            
            // 3. JWT 토큰 생성
            String jwtToken = tokenProvider.createToken(user.getEmail());
            String refreshToken = tokenProvider.createRefreshToken(user.getEmail());
            
            // 4. 리프레시 토큰 Redis 저장
            redisTemplate.opsForValue().set(
                "RT:" + refreshToken,
                user.getEmail(),
                tokenProvider.getRefreshTokenValidityInMilliseconds(),
                TimeUnit.MILLISECONDS
            );
            
            // 5. 응답 생성 (isSuccess 추가)
            AuthResponse authResponse = AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getTokenValidityInMilliseconds())
                .userProfile(UserProfile.from(user))
                .isSuccess(true)  // isSuccess 필드 설정
                .build();
    
            log.info("Login successful for user: {}", user.getEmail());
            
            return ApiResponse.success("Kakao login successful", authResponse);
            
        } catch (Exception e) {
            log.error("Kakao OAuth2 login failed", e);
            throw new RuntimeException("Failed to process Kakao login", e);
        }
    }

    @Operation(summary = "네이버 로그인", description = "네이버 OAuth2 로그인을 처리합니다.")
    @GetMapping("/callback/naver")
    public ApiResponse<AuthResponse> naverCallback(
            @RequestParam String code,
            @RequestParam String state) {
        try {
            log.info("Naver OAuth2 callback received with code: {} and state: {}", code, state);
            
            // 1. 네이버 액세스 토큰 획득
            String accessToken = naverService.getAccessToken(code, state);
            log.info("Naver access token obtained: {}", accessToken);
            
            // 2. 사용자 정보 조회 또는 생성
            User user = naverService.getOrCreateUser(accessToken);
            log.info("User info obtained: {}", user);
            
            // 3. JWT 토큰 생성
            String jwtToken = tokenProvider.createToken(user.getEmail());
            String refreshToken = tokenProvider.createRefreshToken(user.getEmail());
            
            // 4. 리프레시 토큰 Redis 저장
            redisTemplate.opsForValue().set(
                "RT:" + refreshToken,
                user.getEmail(),
                tokenProvider.getRefreshTokenValidityInMilliseconds(),
                TimeUnit.MILLISECONDS
            );
            
            // 5. 응답 생성
            AuthResponse authResponse = AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getTokenValidityInMilliseconds())
                .userProfile(UserProfile.from(user))
                .build();
    
            return ApiResponse.success("Naver login successful", authResponse);
            
        } catch (Exception e) {
            log.error("Naver OAuth2 login failed", e);
            throw new RuntimeException("Failed to process Naver login: " + e.getMessage(), e);
        }
    }
}