package com.example.TripSpring.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

import com.example.TripSpring.dto.domain.RouteSegment;

@Data
@Builder
public class RouteOptionsResponse {
   private List<RouteSegment> segments;
   private TotalOptions totalOptions;
   
   @Data
   @Builder
   public static class TotalOptions {
       private RouteOption fastest;
       private RouteOption cheapest;
   }
}