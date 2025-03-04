//src/main/java/com/example/TripSpring/dto/navigation/NavigationResponse.java
package com.example.TripSpring.dto.navigation;

import com.example.TripSpring.dto.domain.Location;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class NavigationResponse {
    private String navigationId;
    private NavigationStatus status;
    private Location currentLocation;
    private Location nextWaypoint;
    private String currentInstruction;
    private List<String> upcomingInstructions;
    private int remainingDistance;
    private int remainingTime;
    private boolean rerouting;
    private List<NavigationAlert> alerts;

    @Data
    @Builder
    public static class NavigationAlert {
        private AlertType type;
        private String message;
        private AlertSeverity severity;

        public enum AlertType {
            TRAFFIC, ACCIDENT, REROUTE, ARRIVAL, DELAY
        }

        public enum AlertSeverity {
            INFO, WARNING, CRITICAL
        }
    }

    public enum NavigationStatus {
        ACTIVE, PAUSED, REROUTING, COMPLETED, ERROR
    }
}
