package com.example.TripSpring.service.calculator;

import com.example.TripSpring.dto.domain.Schedule;
import com.example.TripSpring.dto.domain.TrafficInfo;
import com.example.TripSpring.dto.response.RouteMetrics;
import com.example.TripSpring.service.FirstMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RouteMetricsCalculator {
    private final FirstMapService firstMapService;
    private static final double BASE_FARE = 3800; // 기본 택시 요금
    private static final double FARE_PER_KM = 1000; // km당 요금

    public RouteMetrics calculateMetrics(List<Schedule> schedules) {
        if (schedules == null || schedules.size() < 2) {
            return createEmptyMetrics();
        }

        double totalDistance = calculateTotalDistance(schedules);
        int totalTravelTime = calculateActualTravelTime(schedules);
        double averageTrafficRate = calculateAverageTrafficRate(schedules);
        double totalCost = calculateCost(totalDistance, averageTrafficRate);

        return createMetrics(totalDistance, totalTravelTime, totalCost, averageTrafficRate);
    }

    private RouteMetrics createMetrics(double distance, int time, double cost, double trafficRate) {
        RouteMetrics metrics = new RouteMetrics();
        metrics.setTotalDistance(distance);
        metrics.setTotalTravelTime(time);
        metrics.setTotalCost(cost);
        metrics.setAverageTrafficRate(trafficRate);
        return metrics;
    }

    private RouteMetrics createEmptyMetrics() {
        return createMetrics(0, 0, 0, 1.0);
    }

    private double calculateTotalDistance(List<Schedule> schedules) {
        double totalDistance = 0;
        for (int i = 0; i < schedules.size() - 1; i++) {
            TrafficInfo trafficInfo = getTrafficInfo(schedules.get(i), schedules.get(i + 1));
            totalDistance += trafficInfo.getDistance();
        }
        return totalDistance;
    }

    private int calculateActualTravelTime(List<Schedule> schedules) {
        int totalTime = 0;
        for (int i = 0; i < schedules.size() - 1; i++) {
            TrafficInfo trafficInfo = getTrafficInfo(schedules.get(i), schedules.get(i + 1));
            totalTime += trafficInfo.getEstimatedTime();
        }
        return totalTime;
    }

    private double calculateAverageTrafficRate(List<Schedule> schedules) {
        double totalRate = 0;
        int count = schedules.size() - 1;
        
        if (count <= 0) return 1.0;

        for (int i = 0; i < count; i++) {
            TrafficInfo trafficInfo = getTrafficInfo(schedules.get(i), schedules.get(i + 1));
            totalRate += trafficInfo.getTrafficRate();
        }
        
        return totalRate / count;
    }

    private double calculateCost(double distance, double trafficRate) {
        return BASE_FARE + (distance * FARE_PER_KM * trafficRate);
    }

    private TrafficInfo getTrafficInfo(Schedule from, Schedule to) {
        return firstMapService.getTrafficInfo(from.getLocation(), to.getLocation());
    }
}