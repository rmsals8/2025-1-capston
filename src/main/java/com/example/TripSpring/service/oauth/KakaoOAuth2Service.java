package com.example.TripSpring.service.oauth;

import com.example.TripSpring.domain.auth.AuthProvider;
import com.example.TripSpring.domain.auth.Role;
import com.example.TripSpring.domain.user.User;
import com.example.TripSpring.payload.oauth2.KakaoUserInfo;
import com.example.TripSpring.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoOAuth2Service {
    private final RestTemplate restTemplate;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    // private final String REDIRECT_URI = "https://43b2-203-250-67-107.ngrok-free.app/api/oauth2/callback/kakao";
    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String REDIRECT_URI;
    
    
    @Value("${app.api.kakao}")
    private String clientId;
    
    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String clientSecret;

    public String getAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId); 
        params.add("redirect_uri", REDIRECT_URI);
        params.add("code", code);
        params.add("client_secret", clientSecret);  // 하드코딩 대신 주입된 값 사용
    
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
    
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                KAKAO_TOKEN_URL,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
    
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Kakao token response: {}", response.getBody());
                return response.getBody().get("access_token").toString();
            } else {
                throw new RuntimeException("Failed to get access token from Kakao");
            }
        } catch (Exception e) {
            log.error("Error getting Kakao access token", e);
            throw new RuntimeException("Failed to get Kakao access token", e);
        }
    }

    public User getOrCreateUser(String accessToken) {
        // 기존 KakaoUserInfo의 from() 메서드를 그대로 사용
        KakaoUserInfo kakaoUserInfo = getUserInfo(accessToken);
    
        return userRepository.findByProviderId(kakaoUserInfo.getId())
            .orElseGet(() -> createKakaoUser(kakaoUserInfo));
    }
    private User createKakaoUser(KakaoUserInfo kakaoUserInfo) {
        return userRepository.save(User.builder()
            .email(kakaoUserInfo.getEmail() != null ? 
                kakaoUserInfo.getEmail() : 
                kakaoUserInfo.getId() + "@kakao.com")
            .name(kakaoUserInfo.getName())
            .providerId(kakaoUserInfo.getId())
            .provider(AuthProvider.KAKAO)
            .role(Role.ROLE_USER)
            .profileImage(kakaoUserInfo.getProfileImageUrl())
            .build());
    }
    public KakaoUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    
        HttpEntity<?> entity = new HttpEntity<>(headers);
    
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                KAKAO_USER_INFO_URL,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
    
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return KakaoUserInfo.from(response.getBody());
            } else {
                throw new OAuth2AuthenticationException("Failed to get Kakao user info");
            }
        } catch (Exception e) {
            log.error("Error getting Kakao user info", e);
            throw new OAuth2AuthenticationException(null, "Failed to get Kakao user info", e);
        }
    }

    private User createKakaoUser(Map<String, Object> userInfo) {
        Map<String, Object> properties = (Map<String, Object>) userInfo.get("properties");
        Map<String, Object> kakaoAccount = (Map<String, Object>) userInfo.get("kakao_account");

        String email = kakaoAccount.containsKey("email") ? 
            kakaoAccount.get("email").toString() : 
            userInfo.get("id") + "@kakao.com";

        return userRepository.save(User.builder()
            .email(email)
            .name(properties.get("nickname").toString())
            .providerId(userInfo.get("id").toString())
            .provider(AuthProvider.KAKAO)
            .role(Role.ROLE_USER)
            .profileImage(properties.get("profile_image").toString())
            .build());
    }
}
