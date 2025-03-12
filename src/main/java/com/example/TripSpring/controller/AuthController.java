package com.example.TripSpring.controller;
import com.example.TripSpring.service.SocialLoginService;
import com.example.TripSpring.service.oauth.KakaoOAuth2Service;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.TripSpring.payload.request.LoginRequest;
import com.example.TripSpring.payload.request.SignupRequest;
import com.example.TripSpring.payload.request.SocialLoginRequest;
import com.example.TripSpring.payload.request.TokenRefreshRequest;
import com.example.TripSpring.payload.response.AuthResponse;
import com.example.TripSpring.payload.response.MessageResponse;
import com.example.TripSpring.security.CurrentUser;
import com.example.TripSpring.security.JwtTokenProvider;
import com.example.TripSpring.security.UserPrincipal;
import com.example.TripSpring.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final SocialLoginService socialLoginService;
    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final KakaoOAuth2Service kakaoOAuth2Service;

    
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }
    @PostMapping("/social/kakao")
    public ResponseEntity<AuthResponse> kakaoLogin(@Valid @RequestBody SocialLoginRequest request) {
        return ResponseEntity.ok(socialLoginService.loginWithKakao(request));
    }

    @PostMapping("/social/naver")
    public ResponseEntity<AuthResponse> naverLogin(@Valid @RequestBody SocialLoginRequest request) {
        return ResponseEntity.ok(socialLoginService.loginWithNaver(request));
    }
    // @PostMapping("/verify-phone")
    // public ResponseEntity<VerificationResponse> verifyPhone(
    //         @Valid @RequestBody PhoneVerificationRequest request) {
    //     return ResponseEntity.ok(authService.verifyPhone(request));
    // }

    // @PostMapping("/reset-password")
    // public ResponseEntity<MessageResponse> resetPassword(
    //         @Valid @RequestBody PasswordResetRequest request) {
    //     authService.resetPassword(request);
    //     return ResponseEntity.ok(new MessageResponse("Password reset email sent"));
    // }

    // @PutMapping("/change-password")
    // @PreAuthorize("isAuthenticated()")
    // public ResponseEntity<MessageResponse> changePassword(
    //         @Valid @RequestBody PasswordChangeRequest request,
    //         @CurrentUser UserPrincipal currentUser) {
    //     authService.changePassword(request, currentUser);
    //     return ResponseEntity.ok(new MessageResponse("Password changed successfully"));
    // }

    // @PostMapping("/logout")
    // @PreAuthorize("isAuthenticated()")
    // public ResponseEntity<MessageResponse> logout(
    //         @CurrentUser UserPrincipal currentUser,
    //         @RequestHeader("Authorization") String token) {
    //     authService.logout(currentUser, token.substring(7));
    //     return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    // }
}