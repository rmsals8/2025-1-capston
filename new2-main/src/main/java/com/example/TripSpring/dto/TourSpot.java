package com.example.TripSpring.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TourSpot {
    private String contentId;
    private String contentTypeId;
    private String title;
    private String addr1;
    private String addr2;
    private String firstImage;
    private Double mapX;
    private Double mapY;
    private String overview;
}