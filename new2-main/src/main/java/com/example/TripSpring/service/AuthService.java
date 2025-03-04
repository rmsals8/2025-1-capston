package com.example.TripSpring.service;

import com.example.TripSpring.domain.auth.AuthProvider;
import com.example.TripSpring.domain.auth.Role;
import com.example.TripSpring.domain.user.User;
import com.example.TripSpring.exception.BadRequestException;
import com.example.TripSpring.exception.ResourceNotFoundException;
import com.example.TripSpring.payload.request.LoginRequest;
import com.example.TripSpring.payload.request.SignupRequest;
import com.example.TripSpring.payload.request.TokenRefreshRequest;
import com.example.TripSpring.payload.response.AuthResponse;
import com.example.TripSpring.payload.response.UserProfile;
import com.example.TripSpring.repository.UserRepository;
import com.example.TripSpring.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    public AuthResponse signup(SignupRequest request) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("이미 사용중인 이메일입니다.");
        }

        // 전화번호 중복 체크
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BadRequestException("이미 사용중인 전화번호입니다.");
        }

        // 사용자 생성
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phoneNumber(request.getPhoneNumber())
                .provider(AuthProvider.LOCAL)
                .role(Role.ROLE_USER)
                .build();

        userRepository.save(user);

        // 토큰 생성 및 반환
        return authenticateUser(request.getEmail(), request.getPassword());
    }

    public AuthResponse login(LoginRequest request) {
        return authenticateUser(request.getEmail(), request.getPassword());
    }

    public AuthResponse refresh(TokenRefreshRequest request) {
        // Redis에서 리프레시 토큰 검증
        String savedToken = redisTemplate.opsForValue().get("RT:" + request.getRefreshToken());
        if (savedToken == null) {
            throw new BadRequestException("Invalid refresh token");
        }

        // 토큰에서 사용자 정보 추출
        String username = tokenProvider.getUsername(request.getRefreshToken());
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", username));

        // 새 토큰 생성
        String newAccessToken = tokenProvider.createToken(username);
        String newRefreshToken = tokenProvider.createRefreshToken(username);

        // Redis 업데이트
        redisTemplate.delete("RT:" + request.getRefreshToken());
        redisTemplate.opsForValue().set(
            "RT:" + newRefreshToken,
            username,
            tokenProvider.getRefreshTokenValidityInMilliseconds(),
            TimeUnit.MILLISECONDS
        );

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getTokenValidityInMilliseconds())
                .userProfile(UserProfile.from(user))
                .build();
    }

    public void logout(String accessToken, String refreshToken) {
        // Access Token 블랙리스트에 추가
        String username = tokenProvider.getUsername(accessToken);
        long expiration = tokenProvider.getExpirationFromToken(accessToken);
        redisTemplate.opsForValue().set(
            "BL:" + accessToken,
            "logout",
            expiration,
            TimeUnit.MILLISECONDS
        );

        // Refresh Token 삭제
        redisTemplate.delete("RT:" + refreshToken);
    }

    private AuthResponse authenticateUser(String email, String password) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(email, password)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        String accessToken = tokenProvider.createToken(authentication);
        String refreshToken = tokenProvider.createRefreshToken(authentication.getName());

        // Refresh Token을 Redis에 저장
        redisTemplate.opsForValue().set(
            "RT:" + refreshToken,
            authentication.getName(),
            tokenProvider.getRefreshTokenValidityInMilliseconds(),
            TimeUnit.MILLISECONDS
        );

        user.updateLastLogin();
        userRepository.save(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getTokenValidityInMilliseconds())
                .userProfile(UserProfile.from(user))
                .build();
    }
}