package com.example.TripSpring.dto;

import com.example.TripSpring.dto.domain.Schedule;
import com.example.TripSpring.service.ScheduleOptimizationService.TimeSlot;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceWithTimeSlot {
    private Place place;
    private TimeSlot timeSlot;
    private Schedule previousSchedule;
    private Schedule nextSchedule;
    private boolean betweenFixed = false;  // 추가된 필드
    // 새로운 생성자 직접 정의
    public PlaceWithTimeSlot(Place place, TimeSlot timeSlot, Schedule previousSchedule, Schedule nextSchedule) {
        this.place = place;
        this.timeSlot = timeSlot;
        this.previousSchedule = previousSchedule;
        this.nextSchedule = nextSchedule;
        this.betweenFixed = (previousSchedule != null && nextSchedule != null);
    }
        // 새로 추가된 필드의 getter/setter
    public boolean isBetweenFixed() {
        return betweenFixed;
    }
    
    public void setBetweenFixed(boolean betweenFixed) {
        this.betweenFixed = betweenFixed;
    }
}