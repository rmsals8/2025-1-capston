package com.example.TripSpring.dto.response.navigation;


import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class RouteOptionsResponse {
   private List<com.example.TripSpring.dto.response.navigation.RouteSegment> segments;
   private TotalOptions totalOptions;
}