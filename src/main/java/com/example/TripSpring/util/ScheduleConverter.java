package com.example.TripSpring.util;

import com.example.TripSpring.dto.domain.Schedule;
import com.example.TripSpring.dto.domain.Location;
import com.example.TripSpring.dto.domain.PlaceCategory;
import com.example.TripSpring.dto.domain.ScheduleType;
import com.example.TripSpring.dto.domain.route.TransportMode;
import com.example.TripSpring.dto.request.route.RouteRecommendationRequest.OptimizedSchedule;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ScheduleConverter {
    public static List<Schedule> convertToSchedules(List<OptimizedSchedule> optimizedSchedules) {
        if (optimizedSchedules == null) return new ArrayList<>();
        return optimizedSchedules.stream()
            .map(ScheduleConverter::convertToSchedule)
            .collect(Collectors.toList());
    }

    public static Schedule convertToSchedule(OptimizedSchedule optimizedSchedule) {
        if (optimizedSchedule == null) return null;

        return Schedule.builder()
            .name(optimizedSchedule.getName())
            .startTime(optimizedSchedule.getStartTime())
            .endTime(optimizedSchedule.getEndTime())
            .location(convertLocation(optimizedSchedule.getLocation()))
            .type(ScheduleType.FIXED)  // 기본값 설정
            .priority(1)               // 기본값 설정
            .category(determineCategoryFromName(optimizedSchedule.getName()))
            .estimatedDuration(60)     // 기본값 설정 (60분)
            .expectedCost(0.0)         // 기본값 설정
            .visitPreference(createDefaultVisitPreference())
            .constraints(createDefaultConstraints())
            .build();
    }

    private static Location convertLocation(com.example.TripSpring.dto.request.route.RouteRecommendationRequest.Location location) {
        if (location == null) return null;
        return new Location(location.getLatitude(), location.getLongitude());
    }

    private static PlaceCategory determineCategoryFromName(String name) {
        String nameLower = name.toLowerCase();
        if (nameLower.contains("박물관") || nameLower.contains("미술관")) {
            return PlaceCategory.CULTURE;
        } else if (nameLower.contains("쇼핑") || nameLower.contains("몰") || nameLower.contains("시장")) {
            return PlaceCategory.SHOPPING;
        } else if (nameLower.contains("식당") || nameLower.contains("카페")) {
            return PlaceCategory.FOOD;
        } else if (nameLower.contains("공원") || nameLower.contains("산책")) {
            return PlaceCategory.NATURE;
        } else if (nameLower.contains("궁") || nameLower.contains("타워") || nameLower.contains("광장")) {
            return PlaceCategory.LANDMARK;
        }
        return PlaceCategory.ENTERTAINMENT;
    }

    private static Schedule.VisitPreference createDefaultVisitPreference() {
        return new Schedule.VisitPreference(
            false,                  // avoidCrowds
            true,                   // preferIndoor
            TransportMode.WALK,     // preferredTransportMode
            500,                    // maxWalkingDistance (m)
            50000.0                 // maxBudget (원)
        );
    }

    private static Schedule.ScheduleConstraints createDefaultConstraints() {
        return new Schedule.ScheduleConstraints(
            null,                   // earliestStartTime
            null,                   // latestEndTime
            false,                  // requiresWeekend
            60,                     // minimumDuration (분)
            5.0                     // maxTravelDistance (km)
        );
    }

    public static List<OptimizedSchedule> convertToOptimizedSchedules(List<Schedule> schedules) {
        if (schedules == null) return new ArrayList<>();
        return schedules.stream()
            .map(ScheduleConverter::convertToOptimizedSchedule)
            .collect(Collectors.toList());
    }

    public static OptimizedSchedule convertToOptimizedSchedule(Schedule schedule) {
        if (schedule == null) return null;

        OptimizedSchedule optimizedSchedule = new OptimizedSchedule();
        optimizedSchedule.setName(schedule.getName());
        optimizedSchedule.setStartTime(schedule.getStartTime());
        optimizedSchedule.setEndTime(schedule.getEndTime());
        optimizedSchedule.setLocation(convertToDtoLocation(schedule.getLocation()));
        return optimizedSchedule;
    }

    private static com.example.TripSpring.dto.request.route.RouteRecommendationRequest.Location convertToDtoLocation(Location location) {
        if (location == null) return null;
        com.example.TripSpring.dto.request.route.RouteRecommendationRequest.Location dtoLocation = 
            new com.example.TripSpring.dto.request.route.RouteRecommendationRequest.Location();
        dtoLocation.setLatitude(location.getLatitude());
        dtoLocation.setLongitude(location.getLongitude());
        return dtoLocation;
    }
}