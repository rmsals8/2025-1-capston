package com.example.TripSpring.service.calculator;

import com.example.TripSpring.dto.domain.Location;
import com.example.TripSpring.dto.domain.TrafficInfo;
import com.example.TripSpring.service.FirstMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class TravelTimeCalculator {
    private final FirstMapService firstMapService;
    private static final int MINUTES_PER_KM = 3; // 1km당 3분 (평균 20km/h)
    private static final int MAX_REASONABLE_TIME = 180; // 최대 3시간

    public int calculateTravelTime(Location start, Location end) {
        TrafficInfo trafficInfo = firstMapService.getTrafficInfo(start, end);
        return adjustTravelTime(trafficInfo);
    }

    public LocalDateTime calculateArrivalTime(LocalDateTime departureTime, Location start, Location end) {
        int travelTimeMinutes = calculateTravelTime(start, end);
        return departureTime.plusMinutes(travelTimeMinutes);
    }

    private int adjustTravelTime(TrafficInfo trafficInfo) {
        if (trafficInfo.getEstimatedTime() > MAX_REASONABLE_TIME) {
            // 비현실적인 이동시간 보정
            return (int)(trafficInfo.getDistance() * MINUTES_PER_KM);
        }
        return trafficInfo.getEstimatedTime();
    }
}