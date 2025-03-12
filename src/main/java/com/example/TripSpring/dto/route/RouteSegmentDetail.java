//src/main/java/com/example/TripSpring/dto/route/RouteSegmentDetail.java
package com.example.TripSpring.dto.route;

import com.example.TripSpring.dto.domain.Location;
import com.example.TripSpring.dto.domain.route.TransportMode;
import com.example.TripSpring.dto.domain.route.GeoPoint;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class RouteSegmentDetail {
    private String segmentId;
    private String startLocationName;
    private String endLocationName;
    private Location startLocation;  // 추가
    private Location endLocation;    // 추가
    private TransportMode mode;
    private List<GeoPoint> path;
    private List<RouteStep> steps;
    private double distance;
    private int duration;
    private double congestion;
    private String instruction;
}