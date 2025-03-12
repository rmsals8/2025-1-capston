// src/main/java/com/example/TripSpring/service/SocialLoginService.java

package com.example.TripSpring.service;

import com.example.TripSpring.domain.auth.AuthProvider;
import com.example.TripSpring.domain.auth.Role;
import com.example.TripSpring.domain.user.User;
import com.example.TripSpring.payload.oauth2.KakaoUserInfo;
import com.example.TripSpring.payload.oauth2.NaverUserInfo;
import com.example.TripSpring.payload.request.SocialLoginRequest;
import com.example.TripSpring.payload.response.AuthResponse;
import com.example.TripSpring.payload.response.UserProfile;
import com.example.TripSpring.repository.UserRepository;
import com.example.TripSpring.security.JwtTokenProvider;
import com.example.TripSpring.service.oauth.KakaoOAuth2Service;
import com.example.TripSpring.service.oauth.NaverOAuth2Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SocialLoginService {
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final KakaoOAuth2Service kakaoService;
    private final NaverOAuth2Service naverService;
    public AuthResponse loginWithNaver(SocialLoginRequest request) {
        try {
            // 네이버 사용자 정보 조회
            NaverUserInfo userInfo = naverService.getUserInfo(request.getAccessToken());
            
            // 기존 사용자 조회
            Optional<User> existingUser = userRepository.findByEmailAndProvider(
                userInfo.getEmail(), 
                AuthProvider.NAVER
            );
            
            User user;
            boolean isNewUser = false;

            if (existingUser.isPresent()) {
                user = existingUser.get();
            } else {
                // 신규 사용자 생성
                user = User.builder()
                    .email(userInfo.getEmail())
                    .name(userInfo.getName())
                    .profileImage(userInfo.getProfileImageUrl())
                    .provider(AuthProvider.NAVER)
                    .providerId(userInfo.getId())
                    .role(Role.ROLE_USER)
                    .build();
                isNewUser = true;
            }

            // 전화번호 검증이 필요한 경우
            if (isNewUser && request.getPhoneNumber() != null) {
                user.setPhoneNumber(request.getPhoneNumber());
            }

            user.updateLastLogin();
            user = userRepository.save(user);

            // 토큰 생성
            String accessToken = tokenProvider.createToken(user.getEmail());
            String refreshToken = tokenProvider.createRefreshToken(user.getEmail());

            return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getTokenValidityInMilliseconds())
                .userProfile(UserProfile.from(user))
                .build();

        } catch (Exception e) {
            log.error("Naver login failed", e);
            throw new OAuth2AuthenticationException(null, "Failed to process Naver login", e);
        }
    }
    public AuthResponse loginWithKakao(SocialLoginRequest request) {
        try {
            // 카카오 사용자 정보 조회
            KakaoUserInfo userInfo = kakaoService.getUserInfo(request.getAccessToken());
            
            // 기존 사용자 조회
            Optional<User> existingUser = userRepository.findByEmailAndProvider(
                userInfo.getEmail(), 
                AuthProvider.KAKAO
            );
            
            User user;
            boolean isNewUser = false;

            if (existingUser.isPresent()) {
                user = existingUser.get();
            } else {
                // 신규 사용자 생성
                user = User.builder()
                    .email(userInfo.getEmail())
                    .name(userInfo.getName())
                    .profileImage(userInfo.getProfileImageUrl())
                    .provider(AuthProvider.KAKAO)
                    .providerId(userInfo.getId())
                    .role(Role.ROLE_USER)
                    .build();
                isNewUser = true;
            }

            // 전화번호 검증이 필요한 경우
            if (isNewUser && request.getPhoneNumber() != null) {
                user.setPhoneNumber(request.getPhoneNumber());
            }

            user.updateLastLogin();
            user = userRepository.save(user);

            // 토큰 생성
            String accessToken = tokenProvider.createToken(user.getEmail());
            String refreshToken = tokenProvider.createRefreshToken(user.getEmail());

            return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getTokenValidityInMilliseconds())
                .userProfile(UserProfile.from(user))
                .build();

        } catch (Exception e) {
            log.error("Kakao login failed", e);
            throw new OAuth2AuthenticationException(null, "Failed to process Kakao login", e);
        }
    }

    private User createSocialUser(KakaoUserInfo userInfo, AuthProvider provider) {
        return User.builder()
            .email(userInfo.getEmail())
            .name(userInfo.getName())
            .profileImage(userInfo.getProfileImageUrl())
            .provider(provider)
            .providerId(userInfo.getId())
            .role(Role.ROLE_USER)
            .build();
    }
}