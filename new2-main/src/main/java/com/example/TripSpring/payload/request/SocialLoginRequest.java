// src/main/java/com/example/TripSpring/payload/request/SocialLoginRequest.java

package com.example.TripSpring.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SocialLoginRequest {
    @NotBlank(message = "액세스 토큰은 필수입니다.")
    private String accessToken;
    
    // 선택적 필드들
    private String phoneNumber;  // 전화번호 인증이 필요한 경우
    private String name;        // 소셜 플랫폼에서 제공하지 않는 경우
    private boolean agreedToTerms;  // 약관 동의 여부
    
    // 추가 정보가 필요할 경우를 위한 필드
    private String deviceToken;  // FCM 토큰 등
    private String deviceType;   // 기기 종류 (Android/iOS)
}