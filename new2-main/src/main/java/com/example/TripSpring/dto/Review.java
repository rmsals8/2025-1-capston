package com.example.TripSpring.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Review {
    private String author_name;
    private Integer rating;
    private String text;
    private String time;
}