package com.example.TripSpring.dto.domain;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter 
@Setter
public class TimeWindow {
    private LocalDateTime start;
    private LocalDateTime end;
    private Location previousLocation;
    private Location nextLocation;
}