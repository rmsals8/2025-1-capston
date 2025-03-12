package com.example.TripSpring.dto.foursquare;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Hours {
    private String status;
    private List<OpeningHours> regular;
}