// src/main/java/com/example/TripSpring/dto/VisitHistoryDto.java
package com.example.TripSpring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitHistoryDto {
    
    private String placeName;
    private String placeId;
    private String category;
    private Double latitude;
    private Double longitude;
    private String address;
}