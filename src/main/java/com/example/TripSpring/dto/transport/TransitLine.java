// src/main/java/com/example/TripSpring/dto/transport/TransitLine.java
package com.example.TripSpring.dto.transport;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransitLine {
    private String lineId;
    private String lineName;
    private String direction;
    private int nextArrivalMinutes;
    private String operator;
    private Double fare;
}