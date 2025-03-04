// src/main/java/com/example/TripSpring/dto/transport/TransitPoint.java
package com.example.TripSpring.dto.transport;

import com.example.TripSpring.dto.domain.route.GeoPoint;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TransitPoint {
    private GeoPoint location;
    private String name;
    private TransitType type;
    private LocalDateTime estimatedTime;
    private List<TransitLine> availableLines;
}
