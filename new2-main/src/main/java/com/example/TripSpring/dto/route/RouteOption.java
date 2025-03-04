//src/main/java/com/example/TripSpring/dto/route/RouteOption.java
package com.example.TripSpring.dto.route;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class RouteOption {
    private String optionId;
    private List<RoutePart> parts;
    private double totalDistance;
    private int totalDuration;
    private double totalCost;
    private int numberOfTransfers;
    private String description;
    private double score;
}