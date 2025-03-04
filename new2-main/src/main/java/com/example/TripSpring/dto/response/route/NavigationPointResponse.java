package com.example.TripSpring.dto.response.route;

import com.example.TripSpring.dto.domain.route.GeoPoint;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NavigationPointResponse {
    private String instruction;    // 안내 메시지
    private GeoPoint point;        // 안내 포인트 위치
}