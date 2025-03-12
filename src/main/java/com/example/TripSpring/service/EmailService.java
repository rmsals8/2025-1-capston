package com.example.TripSpring.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;


    @Value("${app.email.sender}")
    private String sender;

    @Value("${app.email.sender-name:Trip Helper}")
    private String senderName;

    /**
     * 비밀번호 재설정 코드를 이메일로 발송
     */
    @Async
    public void sendPasswordResetCode(String to, String code) {
        try {
            String subject = "[Trip Helper] 비밀번호 재설정 인증번호";
            // Thymeleaf 템플릿 사용 코드 제거
            String content = getBasicPasswordResetEmailContent(code);
            
            sendEmail(to, subject, content);
            log.info("비밀번호 재설정 이메일 발송 완료: {}", to);
        } catch (Exception e) {
            log.error("비밀번호 재설정 이메일 발송 실패: {}", to, e);
            throw new RuntimeException("이메일 발송에 실패했습니다", e);
        }
    }
    /**
     * 비밀번호 변경 성공 알림 이메일 발송
     */
    @Async
    public void sendPasswordChangedNotification(String to) {
        try {
            String subject = "[Trip Helper] 비밀번호가 변경되었습니다";
            String content = getPasswordChangedEmailContent();
            
            sendEmail(to, subject, content);
            log.info("비밀번호 변경 알림 이메일 발송 완료: {}", to);
        } catch (Exception e) {
            log.error("비밀번호 변경 알림 이메일 발송 실패: {}", to, e);
        }
    }
    
    /**
     * 이메일 발송 공통 메소드
     */
private void sendEmail(String to, String subject, String htmlContent) 
        throws MessagingException, UnsupportedEncodingException {
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
    
    helper.setFrom(sender, senderName);
    helper.setTo(to);
    helper.setSubject(subject);
    helper.setText(htmlContent, true); // HTML 형식 사용
    
    mailSender.send(message);
}
    
    /**
     * 기본 비밀번호 재설정 이메일 내용
     */
    private String getBasicPasswordResetEmailContent(String code) {
        return "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "<meta charset=\"UTF-8\">"
                + "<title>비밀번호 재설정 인증번호</title>"
                + "<style>"
                + "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }"
                + ".container { max-width: 600px; margin: 0 auto; padding: 20px; }"
                + ".header { background-color: #3498db; color: white; padding: 15px; text-align: center; }"
                + ".content { padding: 20px; background-color: #f9f9f9; }"
                + ".code { font-size: 32px; font-weight: bold; text-align: center; color: #3498db; margin: 25px 0; letter-spacing: 5px; }"
                + ".footer { text-align: center; margin-top: 20px; font-size: 12px; color: #777; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class=\"container\">"
                + "<div class=\"header\"><h2>Trip Helper 비밀번호 재설정</h2></div>"
                + "<div class=\"content\">"
                + "<p>안녕하세요!</p>"
                + "<p>비밀번호 재설정을 위한 인증번호입니다. 아래 6자리 코드를 입력해주세요:</p>"
                + "<div class=\"code\">" + code + "</div>"
                + "<p>이 인증번호는 5분간 유효합니다.</p>"
                + "<p>비밀번호 재설정을 요청하지 않으셨다면 이 이메일을 무시하세요.</p>"
                + "</div>"
                + "<div class=\"footer\">"
                + "<p>Trip Helper &copy; 2025. All rights reserved.</p>"
                + "<p>본 메일은 발신 전용으로 회신되지 않습니다.</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";
    }
    
    /**
     * 비밀번호 변경 완료 이메일 내용
     */
    private String getPasswordChangedEmailContent() {
        return "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "<meta charset=\"UTF-8\">"
                + "<title>비밀번호 변경 완료</title>"
                + "<style>"
                + "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }"
                + ".container { max-width: 600px; margin: 0 auto; padding: 20px; }"
                + ".header { background-color: #27ae60; color: white; padding: 15px; text-align: center; }"
                + ".content { padding: 20px; background-color: #f9f9f9; }"
                + ".icon { text-align: center; font-size: 48px; margin: 20px 0; color: #27ae60; }"
                + ".footer { text-align: center; margin-top: 20px; font-size: 12px; color: #777; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class=\"container\">"
                + "<div class=\"header\"><h2>Trip Helper 비밀번호 변경 완료</h2></div>"
                + "<div class=\"content\">"
                + "<p>안녕하세요!</p>"
                + "<div class=\"icon\">✓</div>"
                + "<p>회원님의 비밀번호가 성공적으로 변경되었습니다.</p>"
                + "<p>본인이 변경하지 않았다면, 즉시 고객센터로 연락해 주세요.</p>"
                + "</div>"
                + "<div class=\"footer\">"
                + "<p>Trip Helper &copy; 2025. All rights reserved.</p>"
                + "<p>본 메일은 발신 전용으로 회신되지 않습니다.</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";
    }
}