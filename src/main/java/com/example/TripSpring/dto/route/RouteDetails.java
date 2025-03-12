// src/main/java/com/example/TripSpring/dto/route/RouteDetails.java
package com.example.TripSpring.dto.route;
import com.example.TripSpring.dto.transport.TransitPoint;
import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class RouteDetails {
    private String routeId;
    private List<RouteSegmentDetail> segments;
    private Map<String, Double> metrics;  // 거리, 시간, 비용 등
    private List<String> warnings;        // 공사, 통제 등 경고
    private List<TransitPoint> keyPoints; // 주요 경유지
    private String status;                // 경로 상태 (정상, 우회 등)
    private double totalDistance;
    private int totalDuration;
    private double totalCost;
}