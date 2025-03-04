// src/main/java/com/example/TripSpring/dto/response/EnhancedRouteResponse.java
package com.example.TripSpring.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
public class EnhancedRouteResponse {
    private List<TripSegment> segments;
    private List<EnhancedSchedule> schedules;
    private List<TimeSlotAnalysis> timeSlotAnalyses;
    private List<String> optimizationReasons;
    private RouteMetrics metrics;
}







