// src/main/java/com/example/TripSpring/payload/oauth2/KakaoUserInfo.java

package com.example.TripSpring.payload.oauth2;

import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KakaoUserInfo {
    private String id;
    private String email;
    private String name;
    private String profileImageUrl;
    private String phoneNumber;

    public static KakaoUserInfo from(Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        return KakaoUserInfo.builder()
            .id(String.valueOf(attributes.get("id")))
            .email((String) kakaoAccount.get("email"))
            .name((String) profile.get("nickname"))
            .profileImageUrl((String) profile.get("profile_image_url"))
            .phoneNumber((String) kakaoAccount.get("phone_number"))
            .build();
    }
}