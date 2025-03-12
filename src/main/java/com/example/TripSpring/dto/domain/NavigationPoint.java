package com.example.TripSpring.dto.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NavigationPoint {
    private Location location;
    private String instruction;
    private PointType type;
    
    public enum PointType {
        START,
        END,
        WAYPOINT,
        TURN,
        TRANSPORT_CHANGE
    }
}