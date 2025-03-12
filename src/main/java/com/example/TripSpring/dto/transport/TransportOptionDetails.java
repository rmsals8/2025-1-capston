// src/main/java/com/example/TripSpring/dto/transport/TransportOptionDetails.java
package com.example.TripSpring.dto.transport;

import com.example.TripSpring.dto.domain.route.TransportMode;
import com.example.TripSpring.dto.domain.route.GeoPoint;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class TransportOptionDetails {
    private TransportMode mode;
    private double distance;      // 거리 (km)
    private int duration;         // 소요 시간 (분)
    private double cost;          // 비용 (원)
    private double congestion;    // 혼잡도 (0.0 ~ 1.0)
    private String routeDescription;
    private List<GeoPoint> path;
    private List<TransitPoint> transitPoints;
    
    // 편의 메소드
    public boolean isPublicTransport() {
        return mode == TransportMode.BUS || mode == TransportMode.SUBWAY;
    }
    
    public boolean requiresTransfer() {
        return isPublicTransport() && transitPoints != null && transitPoints.size() > 1;
    }
}