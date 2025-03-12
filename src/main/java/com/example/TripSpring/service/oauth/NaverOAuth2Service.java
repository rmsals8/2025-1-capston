// src/main/java/com/example/TripSpring/service/oauth/NaverOAuth2Service.java
package com.example.TripSpring.service.oauth;

import com.example.TripSpring.domain.auth.AuthProvider;
import com.example.TripSpring.domain.auth.Role;
import com.example.TripSpring.domain.user.User;
import com.example.TripSpring.payload.oauth2.NaverUserInfo;
import com.example.TripSpring.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverOAuth2Service {
    private final RestTemplate restTemplate;
    private final UserRepository userRepository;   

    @Value("${app.api.naver.client-id2}")
    private String clientId;
    
    @Value("${app.api.naver.client-secret2}")
    private String clientSecret;
    private static final String TOKEN_URL = "https://nid.naver.com/oauth2.0/token";
    private static final String NAVER_API_URL = "https://openapi.naver.com/v1/nid/me";

    public NaverUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                NAVER_API_URL,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return NaverUserInfo.from(response.getBody());
            } else {
                throw new OAuth2AuthenticationException("Failed to get Naver user info");
            }
        } catch (Exception e) {
            log.error("Error getting Naver user info", e);
            throw new OAuth2AuthenticationException(null, "Failed to get Naver user info", e);
        }
    }
    public String getAccessToken(String code, String state) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "authorization_code");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("code", code);
            body.add("state", state);
    
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            
            log.info("Requesting Naver access token with code: {}", code);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                TOKEN_URL,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            log.info("Naver token response: {}", response.getBody());
    
            if (response.getBody() != null && response.getBody().get("access_token") != null) {
                return response.getBody().get("access_token").toString();
            }
            
            throw new OAuth2AuthenticationException("Failed to get access token from Naver");
        } catch (Exception e) {
            log.error("Error getting Naver access token", e);
            throw new OAuth2AuthenticationException(null, "Failed to get Naver access token: " + e.getMessage(), e);
        }
    }

    public User getOrCreateUser(String accessToken) {
        NaverUserInfo userInfo = getUserInfo(accessToken);
        
        // 기존 사용자 조회
        Optional<User> existingUser = userRepository.findByEmailAndProvider(
            userInfo.getEmail(), 
            AuthProvider.NAVER
        );

        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        // 새 사용자 생성
        User newUser = User.builder()
            .email(userInfo.getEmail())
            .name(userInfo.getName())
            .profileImage(userInfo.getProfileImageUrl())
            .provider(AuthProvider.NAVER)
            .providerId(userInfo.getId())
            .role(Role.ROLE_USER)
            .build();

        return userRepository.save(newUser);
    }
}