// PasswordUpdateRequest.java
package com.example.TripSpring.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordUpdateRequest {
    @NotBlank(message = "이메일은 필수 입력사항입니다.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    private String email;
    
    @NotBlank(message = "재설정 토큰은 필수 입력사항입니다.")
    private String resetToken;
    
    @NotBlank(message = "새 비밀번호는 필수 입력사항입니다.")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    private String newPassword;
}