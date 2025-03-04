package com.example.TripSpring.service;

import com.example.TripSpring.domain.user.User;
import com.example.TripSpring.exception.ResourceNotFoundException;
import com.example.TripSpring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // 인증번호 Redis 키 접두사
    private static final String RESET_CODE_PREFIX = "RESET_CODE:";
    // 재설정 토큰 Redis 키 접두사
    private static final String RESET_TOKEN_PREFIX = "RESET_TOKEN:";
    // 코드 만료 시간 (5분)
    private static final long CODE_EXPIRATION = 300;
    // 토큰 만료 시간 (10분)
    private static final long TOKEN_EXPIRATION = 600;

    /**
     * 비밀번호 재설정 코드 생성 및 발송
     */
    public void sendResetCode(String email, String phoneNumber) {
        // 사용자 존재 여부 확인
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        
        // 전화번호 일치 여부 확인
        if (!user.getPhoneNumber().equals(phoneNumber)) {
            throw new IllegalArgumentException("전화번호가 일치하지 않습니다.");
        }
        redisTemplate.delete(RESET_CODE_PREFIX + email);
        // 6자리 인증 코드 생성
        String code = generateRandomCode(6);
        
        // Redis에 인증 코드 저장 (5분 유효)
        redisTemplate.opsForValue().set(
                RESET_CODE_PREFIX + email,
                code,
                CODE_EXPIRATION,
                TimeUnit.SECONDS
        );
        
        log.info("새로운 인증 코드 생성_ new  verificaion code  generaion : {} for email: {}", code, email);
        
        // 이메일로 인증 코드 발송
        emailService.sendPasswordResetCode(email, code);
    }
    
    /**
     * 재설정 코드 검증
     */
    public boolean verifyResetCode(String email, String code) {
        String storedCode = redisTemplate.opsForValue().get(RESET_CODE_PREFIX + email);
        
        log.info("Stored Code: {}, Provided Code: {}", storedCode, code);
        
        if (storedCode == null) {
            log.warn("사용자 {}의 인증 코드가 존재하지 않거나 만료되었습니다.verificaion code doesn't exsit or expired", email);
            return false;
        }
        
        boolean isValid = storedCode.equals(code);
        log.info("인증 코드 검증 결과: {} vierfication result ", isValid);
        
        return isValid;
    }
    
    /**
     * 비밀번호 재설정 토큰 생성
     */
    public String generateResetToken(String email) {
        // 인증 코드 삭제
        redisTemplate.delete(RESET_CODE_PREFIX + email);
        
        // 재설정 토큰 생성
        String resetToken = UUID.randomUUID().toString();
        
        // Redis에 토큰 저장 (10분 유효)
        redisTemplate.opsForValue().set(
                RESET_TOKEN_PREFIX + resetToken,
                email,
                TOKEN_EXPIRATION,
                TimeUnit.SECONDS
        );
        
        log.info("사용자 {}의 비밀번호 재설정 토큰이 생성되었습니다.", email);
        
        return resetToken;
    }
    
    /**
     * 비밀번호 업데이트
     */
    @Transactional
    public boolean updatePassword(String email, String resetToken, String newPassword) {
        // 토큰 검증
        String storedEmail = redisTemplate.opsForValue().get(RESET_TOKEN_PREFIX + resetToken);
        
        if (storedEmail == null || !storedEmail.equals(email)) {
            log.warn("유효하지 않은 재설정 토큰: {}", resetToken);
            return false;
        }
        
        try {
            // 사용자 정보 조회
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
            
            // 비밀번호 암호화 및 업데이트
            String encodedPassword = passwordEncoder.encode(newPassword);
            user.updatePassword(encodedPassword);
            userRepository.save(user);
            
            // 토큰 삭제
            redisTemplate.delete(RESET_TOKEN_PREFIX + resetToken);
            
            log.info("사용자 {}의 비밀번호가 성공적으로 변경되었습니다.", email);
            
            // 비밀번호 변경 알림 이메일 발송
            emailService.sendPasswordChangedNotification(email);
            
            return true;
        } catch (Exception e) {
            log.error("비밀번호 업데이트 중 오류 발생", e);
            return false;
        }
    }
    
    /**
     * 랜덤 숫자 코드 생성
     */
    private String generateRandomCode(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        
        return sb.toString();
    }
}