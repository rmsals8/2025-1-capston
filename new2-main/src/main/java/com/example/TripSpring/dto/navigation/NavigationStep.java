//src/main/java/com/example/TripSpring/dto/navigation/NavigationStep.java
package com.example.TripSpring.dto.navigation;

import com.example.TripSpring.dto.domain.route.TransportMode;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class NavigationStep {
    private TurnByTurnGuide guide;
    private LocalDateTime estimatedTime;
    private TransportMode transportMode;
    private String trafficCondition;
    private String pointOfInterest;
    private boolean isTransferPoint;
    private int remainingDistance;
    private int remainingDuration;
}