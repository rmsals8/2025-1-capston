// RouteSegment.java
package com.example.TripSpring.dto.response.navigation;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class RouteSegment {
   private String fromLocation;
   private String toLocation; 
   private List<TransportOption> options;
}