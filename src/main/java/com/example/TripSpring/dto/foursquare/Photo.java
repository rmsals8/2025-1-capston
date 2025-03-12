package com.example.TripSpring.dto.foursquare;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Photo {
    private String id;
    private String prefix;
    private String suffix;
    private int width;
    private int height;
}