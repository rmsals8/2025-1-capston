//src/main/java/com/example/TripSpring/dto/route/RoutePreferences.java
package com.example.TripSpring.dto.route;

import com.example.TripSpring.dto.domain.route.TransportMode;
import lombok.Builder;
import lombok.Data;
import java.util.Set;

@Data
@Builder
public class RoutePreferences {
    private Set<TransportMode> preferredModes;
    private boolean minimizeCost;
    private boolean minimizeTime;
    private boolean minimizeTransfers;
    private int maxWalkingDistance;  // meters
    private double maxCost;
    private Integer maxDuration;     // minutes
}