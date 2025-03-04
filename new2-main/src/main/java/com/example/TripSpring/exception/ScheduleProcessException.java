package com.example.TripSpring.exception;

public class ScheduleProcessException extends RuntimeException {
    public ScheduleProcessException(String message) {
        super(message);
    }
    
    public ScheduleProcessException(String message, Throwable cause) {
        super(message, cause);
    }
}