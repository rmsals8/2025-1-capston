package com.example.TripSpring.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    public static ApiErrorResponse of(int status, String error, String message) {
        return new ApiErrorResponse(
            LocalDateTime.now(),
            status,
            error,
            message,
            "/api/v1/routes/recommended-path"
        );
    }
}