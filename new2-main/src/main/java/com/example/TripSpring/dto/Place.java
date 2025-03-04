package com.example.TripSpring.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class Place {
    private String place_id;
    private String name;
    private String formatted_address;
    private Geometry geometry;
    private Double rating;
    private Integer user_ratings_total;
    private List<String> types;
    private Boolean open_now;
    private String business_status;
    
    // 추가 필드
    private LocalDateTime optimalStartTime; // 최적 방문 시간
    private Double score; // 평가 점수
    private Map<String, Object> metadata; // 추가 메타데이터
    
    // 편의 메소드
    public double getLatitude() {
        return this.geometry != null ? this.geometry.getLocation().getLat() : 0;
    }
    
    public double getLongitude() {
        return this.geometry != null ? this.geometry.getLocation().getLng() : 0;
    }
    
    public String getLocation() {
        return this.formatted_address;
    }
}