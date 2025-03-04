package com.example.TripSpring.dto.request;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LocationUpdate {
    private Double latitude;
    private Double longitude;
    private Double heading;        // 방향 (각도)
    private Double speed;          // 속도 (m/s)
    private Double accuracy;       // 정확도 (미터)
    private LocalDateTime timestamp;
}