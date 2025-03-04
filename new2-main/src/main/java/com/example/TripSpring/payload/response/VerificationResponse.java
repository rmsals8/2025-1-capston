// VerificationResponse.java
package com.example.TripSpring.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResponse {
    private boolean isValid;
    private String resetToken;
}