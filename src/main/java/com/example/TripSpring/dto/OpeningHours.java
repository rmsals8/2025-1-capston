package com.example.TripSpring.dto;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OpeningHours {
    private Boolean open_now;
    private List<String> weekday_text;
}