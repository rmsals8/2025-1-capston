package com.example.TripSpring.dto.response.route;

import java.util.List;

import com.example.TripSpring.dto.domain.route.GeoPoint;
import com.example.TripSpring.dto.domain.route.TransportMode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteSegmentDetailResponse {
    private String fromLocation;                    // 출발지
    private String toLocation;                      // 도착지
    private double distance;                        // 거리 (km)
    private int duration;                           // 소요 시간 (분)
    private TransportMode transportMode;            // 이동 수단
    private List<GeoPoint> path;                    // 상세 경로 좌표
    private List<NavigationPointResponse> turnByTurn; // 이동 안내 정보
    private TransitInfo transitInfo;  // 대중교통 정보 추가
}