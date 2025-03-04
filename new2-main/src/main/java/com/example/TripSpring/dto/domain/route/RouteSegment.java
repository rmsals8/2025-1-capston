package com.example.TripSpring.dto.domain.route;

import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class RouteSegment {
    private GeoPoint startPoint;
    private GeoPoint endPoint;
    private List<GeoPoint> path;
    private TransportMode transportMode;
    private RouteMetrics metrics;
}