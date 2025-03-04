package com.example.TripSpring.dto.foursquare;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpeningHours {
    private List<String> days;
    private String open;
    private String close;
}