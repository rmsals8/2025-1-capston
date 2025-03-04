package com.example.TripSpring.dto.response.route;

import java.time.LocalTime;
import java.util.List;

import com.example.TripSpring.dto.domain.route.CrowdLevel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoutePlaceDetailResponse {
    private String placeName;
    private CrowdLevel crowdLevel;
    private LocalTime bestVisitTime;
    private List<FacilityResponse> nearbyFacilities;
    private EnhancedPlaceDetails enhancedPlaceDetails;
    private List<String> visitTips;
}