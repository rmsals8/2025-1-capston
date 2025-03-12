package com.example.TripSpring.exception;

public class PlaceSearchException extends RuntimeException {
    public PlaceSearchException(String message) {
        super(message);
    }
    
    public PlaceSearchException(String message, Throwable cause) {
        super(message, cause);
    }
}
