package com.example.TripSpring.dto.response;

import com.example.TripSpring.domain.PlaceInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NearbyPlacesResponse {
    private List<PlaceInfo> places;
    private int count;
    private String status = "SUCCESS";
    private String message; // 이미 message 필드가 있는지 확인
    
    // 기존 생성자
    public NearbyPlacesResponse(List<PlaceInfo> places) {
        this.places = places;
        this.count = places.size();
        this.status = "SUCCESS";
    }
    
    // 기존에 없다면 이 메서드만 추가
    public void setNoResults() {
        this.places = new ArrayList<>();
        this.count = 0;
        this.status = "NO_RESULTS";
        this.message = "검색 결과가 없습니다";
    }
}