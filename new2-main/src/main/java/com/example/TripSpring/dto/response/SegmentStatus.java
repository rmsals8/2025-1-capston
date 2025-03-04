package com.example.TripSpring.dto.response;

import com.example.TripSpring.dto.domain.route.TransportMode;

import java.util.List;

import com.example.TripSpring.dto.domain.Location;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SegmentStatus {
    private Location fromLocation;
    private Location toLocation;
    private TransportMode transportMode;
    private double distance;
    private int estimatedDuration;
    private Double progress;
    private String currentInstruction;
    private List<String> turnByTurn;
    private TransitInfo transitInfo;

    // 기본 필드만을 위한 생성자 추가
    public SegmentStatus(Location fromLocation, Location toLocation, 
            TransportMode transportMode, double distance, int estimatedDuration) {
        this.fromLocation = fromLocation;
        this.toLocation = toLocation;
        this.transportMode = transportMode;
        this.distance = distance;
        this.estimatedDuration = estimatedDuration;
        this.progress = 0.0;
    }
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransitInfo {
        private String lineNumber;    // 노선 번호
        private String nextStop;      // 다음 정류장
        private Integer arrivalTime;  // 도착 예정 시간
    }
}