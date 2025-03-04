package com.example.TripSpring.dto;


import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class PlacesResponse {
    private List<Place> results;
    private String status;
    private String next_page_token;
}