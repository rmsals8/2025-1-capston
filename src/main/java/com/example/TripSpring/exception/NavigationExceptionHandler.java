package com.example.TripSpring.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class NavigationExceptionHandler {

    @ExceptionHandler(NavigationException.class)
    public ResponseEntity<ErrorResponse> handleNavigationException(NavigationException ex) {
        log.error("Navigation error occurred: {}", ex.getMessage());
        
        ErrorResponse response = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "내비게이션 오류",
            ex.getMessage()
        );
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Invalid request: {}", ex.getMessage());
        
        ErrorResponse response = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "잘못된 요청",
            ex.getMessage()
        );
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    private static class ErrorResponse {
        private final int status;
        private final String error;
        private final String message;

        public ErrorResponse(int status, String error, String message) {
            this.status = status;
            this.error = error;
            this.message = message;
        }

        // Getters
        public int getStatus() { return status; }
        public String getError() { return error; }
        public String getMessage() { return message; }
    }
}