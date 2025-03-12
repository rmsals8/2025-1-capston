package com.example.TripSpring.payload.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
// 토큰 갱신 요청 DTO
@Getter
@NoArgsConstructor
public class TokenRefreshRequest {
    @NotBlank(message = "리프레시 토큰은 필수입니다.")
    private String refreshToken;
}