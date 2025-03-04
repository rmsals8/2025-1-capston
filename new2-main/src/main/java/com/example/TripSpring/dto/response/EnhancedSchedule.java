package com.example.TripSpring.dto.response;

import com.example.TripSpring.dto.domain.Schedule;

import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class EnhancedSchedule {
    private Schedule schedule;
    private PlaceDetailsInfo placeDetails;
    private String schedulingReason;
}
