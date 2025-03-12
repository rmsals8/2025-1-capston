package com.example.TripSpring.dto.domain;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import com.example.TripSpring.dto.domain.route.NavigationPoint;
import com.example.TripSpring.dto.domain.route.TransportMode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteSegment {
    private String segmentId;
    private Location startLocation;
    private Location endLocation;
    private TransportMode transportMode;
    private List<NavigationPoint> path;
    private double distance;
    private int estimatedDuration;
    private String initialInstruction;
    private LocalDateTime startTime;
    public Location getExpectedLocation() {
        if (path == null || path.isEmpty()) {
            return startLocation;
        }
        
        // 현재 시간 기준으로 시작 시간으로부터 얼마나 지났는지 계산
        LocalDateTime now = LocalDateTime.now();
        Duration elapsedTime = Duration.between(startTime, now);
        double progressRatio = Math.min(1.0, 
            Math.max(0.0, elapsedTime.getSeconds() / (estimatedDuration * 60.0)));
    
        // 아직 시작하지 않았거나 이미 완료된 경우
        if (progressRatio <= 0) return startLocation;
        if (progressRatio >= 1) return endLocation;
    
        // 현재 위치가 있어야 할 path segment 찾기
        int pathSize = path.size();
        int targetIndex = (int) (progressRatio * (pathSize - 1));
        
        // 해당 세그먼트 내에서의 상세 위치 계산
        NavigationPoint currentPoint = path.get(targetIndex);
        NavigationPoint nextPoint = path.get(Math.min(targetIndex + 1, pathSize - 1));
        
        // 두 점 사이에서의 보간 계산
        double segmentProgress = (progressRatio * (pathSize - 1)) - targetIndex;
        
        double lat = currentPoint.getPoint().getLatitude() + 
            (nextPoint.getPoint().getLatitude() - currentPoint.getPoint().getLatitude()) * segmentProgress;
        double lng = currentPoint.getPoint().getLongitude() + 
            (nextPoint.getPoint().getLongitude() - currentPoint.getPoint().getLongitude()) * segmentProgress;
        
        return new Location(lat, lng);
    }

}