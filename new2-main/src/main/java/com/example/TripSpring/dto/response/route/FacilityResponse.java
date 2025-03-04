package com.example.TripSpring.dto.response.route;

import com.example.TripSpring.dto.domain.route.GeoPoint;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacilityResponse {
    private String name;
    private String type;      // 시설 유형 (식당, 카페, 편의점 등)
    private GeoPoint location;
    private double distance;  // 미터 단위
    private String address;
    private OperatingHours operatingHours;
    private double rating;    // 평점 (0-5)
    private int reviewCount;
}