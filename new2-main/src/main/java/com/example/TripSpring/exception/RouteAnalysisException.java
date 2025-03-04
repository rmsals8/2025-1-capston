package com.example.TripSpring.exception;

/**
 * 경로 분석 중 발생하는 예외를 처리하기 위한 커스텀 예외 클래스
 */
public class RouteAnalysisException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RouteAnalysisException(String message) {
        super(message);
    }

    public RouteAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }

    public RouteAnalysisException(Throwable cause) {
        super(cause);
    }
}