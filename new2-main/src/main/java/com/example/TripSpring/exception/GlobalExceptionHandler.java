package com.example.TripSpring.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.example.TripSpring.dto.response.ApiErrorResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RouteAnalysisException.class)
    public ResponseEntity<ApiErrorResponse> handleRouteAnalysisException(RouteAnalysisException ex) {
        log.error("Route analysis error: {}", ex.getMessage(), ex);
        
        ApiErrorResponse error = ApiErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Route Analysis Error",
            ex.getMessage()
        );
        
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Invalid request: {}", ex.getMessage(), ex);
        
        ApiErrorResponse error = ApiErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Invalid Request",
            ex.getMessage()
        );
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        log.error("Unexpected error", e);
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Internal Server Error");
        errorResponse.put("message", "서버 내부 오류가 발생했습니다.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    @ExceptionHandler(NavigationException.class)
    public ResponseEntity<Map<String, Object>> handleNavigationException(NavigationException e) {
        log.error("Navigation Exception", e);
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Navigation Error");
        errorResponse.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}