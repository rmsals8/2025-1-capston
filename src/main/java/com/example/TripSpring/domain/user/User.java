package com.example.TripSpring.domain.user;
import jakarta.persistence.*;
import lombok.*;
// import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import com.example.TripSpring.domain.auth.AuthProvider;
import com.example.TripSpring.domain.auth.Role;

// // Audit 기능 사용시 필요한 import
// import org.springframework.data.annotation.CreatedDate;
// import org.springframework.data.annotation.LastModifiedDate;
// import org.springframework.data.jpa.domain.support.AuditingEntityListener;
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {
    
     @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)  // password를 null 허용으로 변경
    private String password;   

    @Column(unique = true)
    private String email;

    private String name;

    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    private String providerId;

    @Column(nullable = true)  // 소셜 로그인 시 전화번호가 없을 수 있음
    private String phoneNumber;

    private String profileImage;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String refreshToken;

    private LocalDateTime lastLoginAt;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public User(String email, String password, String name, 
               AuthProvider provider, String providerId, 
               String phoneNumber, String profileImage, Role role) {
        this.email = email;
        this.password = password;  // 소셜 로그인의 경우 null이 될 수 있음
        this.name = name;
        this.provider = provider;
        this.providerId = providerId;
        this.phoneNumber = phoneNumber;
        this.profileImage = profileImage;
        this.role = role;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}