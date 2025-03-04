package com.example.TripSpring.dto.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrafficInfo {
    private double trafficRate;
    private int estimatedTime;
    private double distance;
}