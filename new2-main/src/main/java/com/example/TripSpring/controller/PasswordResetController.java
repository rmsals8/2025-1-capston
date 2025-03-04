package com.example.TripSpring.controller;

import com.example.TripSpring.common.response.ApiResponse;
import com.example.TripSpring.domain.user.User;
import com.example.TripSpring.payload.request.PasswordResetRequest;
import com.example.TripSpring.payload.request.PasswordUpdateRequest;
import com.example.TripSpring.payload.request.VerifyResetCodeRequest;
import com.example.TripSpring.payload.response.MessageResponse;
import com.example.TripSpring.payload.response.VerificationResponse;
import com.example.TripSpring.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/password")
@RequiredArgsConstructor
@Tag(name = "비밀번호 관리", description = "비밀번호 찾기 및 재설정 관련 API")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @Operation(summary = "비밀번호 재설정 요청", description = "사용자 이메일과 전화번호로 비밀번호 재설정 메일 발송")
    @PostMapping("/reset-request")
    public ResponseEntity<ApiResponse<MessageResponse>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        
        log.info("비밀번호 재설정 요청: email={}, phoneNumber={}", 
                request.getEmail(), request.getPhoneNumber());
        
        passwordResetService.sendResetCode(request.getEmail(), request.getPhoneNumber());
        
        return ResponseEntity.ok(ApiResponse.success(
                "인증번호가 발송되었습니다. 이메일 또는 SMS를 확인해주세요.",
                new MessageResponse("인증번호가 발송되었습니다.")
        ));
    }
    
    @Operation(summary = "인증번호 확인", description = "비밀번호 재설정 전 인증번호 확인")
    @PostMapping("/verify-code")
    public ResponseEntity<ApiResponse<VerificationResponse>> verifyResetCode(
            @Valid @RequestBody VerifyResetCodeRequest request) {
        
        log.info("비밀번호 재설정 인증번호 확인: email={}, code={}", 
                request.getEmail(), request.getVerificationCode());
        
        boolean isValid = passwordResetService.verifyResetCode(
                request.getEmail(), request.getVerificationCode());
        
        if (isValid) {
            String resetToken = passwordResetService.generateResetToken(request.getEmail());
            
            // 로그 추가
            log.info("인증 성공 - 이메일: {}, 리셋 토큰: {}", request.getEmail(), resetToken);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "인증번호가 확인되었습니다. 새 비밀번호를 설정해주세요.",
                    new VerificationResponse(true, resetToken)
            ));
        } else {
            // 로그 추가
            log.warn("인증 실패 - 이메일: {}", request.getEmail());
            
            return ResponseEntity.ok(ApiResponse.error(
                HttpStatus.BAD_REQUEST,  
                "인증번호가 일치하지 않거나 만료되었습니다.",
                new VerificationResponse(false, null)
            ));
        }
    }
    
    @Operation(summary = "비밀번호 변경", description = "인증 후 새 비밀번호 설정")
    @PostMapping("/update")
    public ResponseEntity<ApiResponse<MessageResponse>> updatePassword(
            @Valid @RequestBody PasswordUpdateRequest request) {
        
        log.info("비밀번호 변경 요청: email={}", request.getEmail());
        
        boolean isUpdated = passwordResetService.updatePassword(
                request.getEmail(), 
                request.getResetToken(), 
                request.getNewPassword()
        );
        
        if (isUpdated) {
            return ResponseEntity.ok(ApiResponse.success(
                    "비밀번호가 성공적으로 변경되었습니다.",
                    new MessageResponse("비밀번호가 성공적으로 변경되었습니다.")
            ));
        } else {
            return ResponseEntity.ok(ApiResponse.error(
            HttpStatus.BAD_REQUEST,  // HttpStatus 객체 사용
            "비밀번호 변경에 실패했습니다. 토큰이 만료되었거나 유효하지 않습니다.",
            new MessageResponse("비밀번호 변경 실패")
        ));
        }
    }
}