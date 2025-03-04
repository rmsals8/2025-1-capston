// src/main/java/com/example/TripSpring/dto/route/RouteStep.java
package com.example.TripSpring.dto.route;

import com.example.TripSpring.dto.domain.route.GeoPoint;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RouteStep {
    private String instruction;
    private StepType type;
    private GeoPoint location;
    private double distanceToNext;
    private String additionalInfo;
    
    public enum StepType {
        START, END, STRAIGHT, LEFT, RIGHT, UTURN, SLIGHT_LEFT, SLIGHT_RIGHT,
        MERGE, EXIT, TRANSFER, BOARD, ALIGHT
    }
}