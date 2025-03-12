package com.example.TripSpring.dto.domain.route;

public enum TransportMode {
    WALK,
    BUS,
    SUBWAY,
    TAXI;
    
    public boolean isPublicTransit() {
        return this == BUS || this == SUBWAY;
    }
}