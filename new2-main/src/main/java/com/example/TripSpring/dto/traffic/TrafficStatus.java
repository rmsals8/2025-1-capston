//src/main/java/com/example/TripSpring/dto/traffic/TrafficStatus.java
package com.example.TripSpring.dto.traffic;

import com.example.TripSpring.dto.domain.Location;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TrafficStatus {
    private Location location;
    private double congestionLevel;
    private int averageSpeed;
    private String status;
    private List<String> incidents;
    private LocalDateTime timestamp;
}