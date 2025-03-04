package com.example.TripSpring.payload.response;


import com.example.TripSpring.domain.auth.AuthProvider;
import com.example.TripSpring.domain.auth.Role;
import com.example.TripSpring.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
// 사용자 프로필 DTO
@Getter
@Builder
@AllArgsConstructor
public class UserProfile {
    private Long id;
    private String email;
    private String name;
    private String phoneNumber;
    private String profileImage;
    private AuthProvider provider;
    private Role role;

    public static UserProfile from(User user) {
        return UserProfile.builder()
            .id(user.getId())
            .email(user.getEmail())
            .name(user.getName())
            .phoneNumber(user.getPhoneNumber())
            .profileImage(user.getProfileImage())
            .provider(user.getProvider())
            .role(user.getRole())
            .build();
    }
}