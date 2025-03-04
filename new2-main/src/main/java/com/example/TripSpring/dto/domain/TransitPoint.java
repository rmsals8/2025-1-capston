package com.example.TripSpring.dto.domain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransitPoint {
    private Location location;
    private String name;
    private String type;  // 환승역, 버스정류장 등
    private List<String> availableLines;
}