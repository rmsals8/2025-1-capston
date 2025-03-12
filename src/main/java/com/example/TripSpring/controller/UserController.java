package com.example.TripSpring.controller;

import com.example.TripSpring.domain.user.User;
import com.example.TripSpring.security.CurrentUser;
import com.example.TripSpring.security.UserPrincipal;
import com.example.TripSpring.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCurrentUser(@CurrentUser UserPrincipal currentUser) {
        try {
            log.info("현재 인증된 사용자 정보 요청");
            log.info("사용자 ID: {}", currentUser.getId());
            log.info("사용자 이메일: {}", currentUser.getEmail());
    
            User user = userService.getUserById(currentUser.getId());
            
            log.info("조회된 사용자 - 이름: {}, 이메일: {}", user.getName(), user.getEmail());
    
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("name", user.getName());
            response.put("email", user.getEmail());
            response.put("phoneNumber", user.getPhoneNumber());
            response.put("profileImage", user.getProfileImage());
    
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("사용자 정보 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "사용자 정보를 조회할 수 없습니다."));
        }
    }
}