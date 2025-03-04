package com.example.TripSpring.dto.response.navigation;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class RouteOptionSummary {
   private int duration;
   private double cost;
   private List<String> modes;
}