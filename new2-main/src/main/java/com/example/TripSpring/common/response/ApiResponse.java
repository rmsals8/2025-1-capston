package com.example.TripSpring.common.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {
    private int status;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
            HttpStatus.OK.value(),
            "Success",
            data,
            LocalDateTime.now()
        );
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(
            HttpStatus.OK.value(),
            message,
            data,
            LocalDateTime.now()
        );
    }

    public static <T> ApiResponse<T> error(HttpStatus status, String message) {
        return new ApiResponse<>(
            status.value(),
            message,
            null,
            LocalDateTime.now()
        );
    }

    public static <T> ApiResponse<T> error(HttpStatus status, String message, T data) {
        return new ApiResponse<>(
            status.value(),
            message,
            data,
            LocalDateTime.now()
        );
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(
            HttpStatus.CREATED.value(),
            "Created",
            data,
            LocalDateTime.now()
        );
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>(
            HttpStatus.CREATED.value(),
            message,
            data,
            LocalDateTime.now()
        );
    }
}