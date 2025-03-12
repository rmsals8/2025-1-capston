//src/main/java/com/example/TripSpring/dto/route/RouteSequence.java
package com.example.TripSpring.dto.route;

import com.example.TripSpring.dto.transport.TransportOptionDetails;
import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class RouteSequence {
    private String sequenceId;
    private List<OrderedLocation> waypoints;
    private Map<Integer, List<TransportOptionDetails>> segmentOptions;
    private double totalDistance;
    private int totalDuration;
    private List<String> visitationOrder;  // 최적 방문 순서
    private Map<String, Object> optimizationMetrics;
}
