//src/main/java/com/example/TripSpring/dto/response/OptimizeResponse.java
package com.example.TripSpring.dto.response;

import com.example.TripSpring.dto.route.CompleteRoute;
import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class OptimizeResponse {
   private List<CompleteRoute> routes;
   private OptimizeMetrics metrics;
   private Map<String, Object> summary;

   @Data
   @Builder
   public static class OptimizeMetrics {
       private int totalRoutesFound;
       private int filteredRoutes;
       private double averageScore;
       private Map<String, Integer> transportModeDistribution;
   }
}