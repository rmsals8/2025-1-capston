package com.example.TripSpring.dto.scheduler;

import com.example.TripSpring.dto.domain.*;
import com.example.TripSpring.dto.domain.route.TransportMode;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;

@Data
@NoArgsConstructor
public class OptimizeRequest {
    private List<FixedSchedule> fixedSchedules;
    private List<FlexibleSchedule> flexibleSchedules;

    @Data
    @NoArgsConstructor
    public static class FixedSchedule {
        private String name;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Location location;
        private PlaceCategory category;             // 추가
        private Double expectedCost;                // 추가
        private Schedule.VisitPreference visitPreference;  // 추가

        public Schedule toSchedule() {
            return Schedule.builder()
                .name(name)
                .startTime(startTime)
                .endTime(endTime)
                .location(location)
                .type(ScheduleType.FIXED)
                .priority(1)
                .category(category != null ? category : determineCategoryFromName())
                .estimatedDuration((int) Duration.between(startTime, endTime).toMinutes())
                .expectedCost(expectedCost != null ? expectedCost : 0.0)
                .visitPreference(visitPreference != null ? visitPreference : createDefaultVisitPreference())
                .constraints(createDefaultConstraints())
                .build();
        }

        private PlaceCategory determineCategoryFromName() {
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
    }

    @Data
    @NoArgsConstructor
    public static class FlexibleSchedule {
        private String name;
        private Location location;
        private int priority;
        private PlaceCategory category;             // 추가
        private Double expectedCost;                // 추가
        private Schedule.VisitPreference visitPreference;  // 추가
        private Integer estimatedDuration;          // 추가

        public Schedule toSchedule() {
            return Schedule.builder()
                .name(name)
                .startTime(null)
                .endTime(null)
                .location(location)
                .type(ScheduleType.FLEXIBLE)
                .priority(priority)
                .category(category != null ? category : determineCategoryFromName())
                .estimatedDuration(estimatedDuration != null ? estimatedDuration : 60)
                .expectedCost(expectedCost != null ? expectedCost : 0.0)
                .visitPreference(visitPreference != null ? visitPreference : createDefaultVisitPreference())
                .constraints(createDefaultConstraints())
                .build();
        }

        private PlaceCategory determineCategoryFromName() {
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
    }

    // 공통 헬퍼 메서드들
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

    public List<Schedule> getFixedSchedulesAsDomain() {
        List<Schedule> schedules = new ArrayList<>();
        if (fixedSchedules != null) {
            fixedSchedules.forEach(fs -> schedules.add(fs.toSchedule()));
        }
        return schedules;
    }

    public List<Schedule> getFlexibleSchedulesAsDomain() {
        List<Schedule> schedules = new ArrayList<>();
        if (flexibleSchedules != null) {
            flexibleSchedules.forEach(fs -> schedules.add(fs.toSchedule()));
        }
        return schedules;
    }
}