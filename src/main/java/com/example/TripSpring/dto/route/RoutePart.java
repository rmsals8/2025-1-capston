//src/main/java/com/example/TripSpring/dto/route/RoutePart.java
package com.example.TripSpring.dto.route;

import com.example.TripSpring.dto.domain.Location;
import com.example.TripSpring.dto.domain.route.TransportMode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoutePart {
    private Location start;
    private Location end;
    private TransportMode mode;
    private double distance;
    private int duration;
    private double cost;
    private String lineName;  // 버스/지하철 노선
    private String instruction;
}