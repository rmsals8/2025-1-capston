package com.example.TripSpring.dto.response.route;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PeakHours {
    private List<TimeSlot> weekdayPeaks;
    private List<TimeSlot> weekendPeaks;
    private Map<DayOfWeek, List<TimeSlot>> dailyPeaks;
}