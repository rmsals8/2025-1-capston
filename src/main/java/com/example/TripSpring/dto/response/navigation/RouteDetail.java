package com.example.TripSpring.dto.response.navigation;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class RouteDetail {
   private String summary;
   private int duration;
   private double distance;
   private double cost;
   private List<RouteStep> steps;
   private String crowdedness;
}
