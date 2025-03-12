package com.example.TripSpring.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RouteOption {
   private String transportMode;
   private int duration;
   private double distance;
   private double estimatedCost;
}