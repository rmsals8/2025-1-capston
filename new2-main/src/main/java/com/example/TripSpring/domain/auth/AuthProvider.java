package com.example.TripSpring.domain.auth;

public enum AuthProvider {
    LOCAL,    // 일반 이메일/비밀번호 회원가입
    KAKAO,    // 카카오 로그인
    NAVER,    // 네이버 로그인
    GOOGLE;   // 추후 구글 로그인 확장 가능성 고려

    public static AuthProvider fromString(String provider) {
        try {
            return AuthProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            return LOCAL;
        }
    }
}