package com.example.TripSpring.dto.response.navigation;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import com.example.TripSpring.dto.domain.route.TransportMode;

@Data
@Builder
public class TransportOption {
   private TransportMode transportMode;
   private List<RouteDetail> routes;
}