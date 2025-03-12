// VerifyResetCodeRequest.java
package com.example.TripSpring.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyResetCodeRequest {
    @NotBlank(message = "이메일은 필수 입력사항입니다.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    private String email;
    
    @NotBlank(message = "인증번호는 필수 입력사항입니다.")
    private String verificationCode;
}