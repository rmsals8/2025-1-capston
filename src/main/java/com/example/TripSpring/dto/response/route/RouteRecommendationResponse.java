package com.example.TripSpring.dto.response.route;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import com.example.TripSpring.dto.response.RouteAnalysisResponse;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteRecommendationResponse {
    private List<RouteSegmentDetailResponse> segments;  // 구간별 경로 정보
    private List<RoutePlaceDetailResponse> places;      // 장소별 상세 정보
    private RouteAnalysisResponse analysis;             // 전체 경로 분석
}