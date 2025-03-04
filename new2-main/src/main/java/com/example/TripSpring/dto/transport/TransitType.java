// src/main/java/com/example/TripSpring/dto/transport/TransitType.java
package com.example.TripSpring.dto.transport;

public enum TransitType {
    BUS_STOP("버스정류장"),
    SUBWAY_STATION("지하철역"),
    TRANSFER_STATION("환승역"),
    TAXI_STAND("택시승강장");

    private final String description;

    TransitType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}