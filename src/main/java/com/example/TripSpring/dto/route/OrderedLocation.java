//src/main/java/com/example/TripSpring/dto/route/OrderedLocation.java
package com.example.TripSpring.dto.route;

import com.example.TripSpring.dto.domain.Location;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder 
public class OrderedLocation {
    private int sequence;
    private Location location;
    private String name;
    private String description;
    private LocalDateTime estimatedArrival;
    private LocalDateTime estimatedDeparture;
    private int stayDuration;  // 체류 시간(분)
    private boolean isRequired;  // 필수 경유지 여부
}