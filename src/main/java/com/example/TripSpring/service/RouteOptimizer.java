package com.example.TripSpring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.example.TripSpring.dto.domain.Location;
import com.example.TripSpring.dto.domain.PlaceInfo;
import com.example.TripSpring.dto.domain.Route;
import com.example.TripSpring.dto.domain.Schedule;
import com.example.TripSpring.dto.domain.ScheduleType;
import com.example.TripSpring.dto.domain.TimeWindow;
import com.example.TripSpring.dto.domain.TrafficInfo;
import com.example.TripSpring.dto.response.EnhancedRouteResponse;
import com.example.TripSpring.dto.response.EnhancedSchedule;
import com.example.TripSpring.dto.response.PlaceDetailsInfo;
import com.example.TripSpring.dto.response.TimeSlotAnalysis;
import com.example.TripSpring.dto.response.TripSegment;
import com.example.TripSpring.service.calculator.TravelTimeCalculator;
import com.example.TripSpring.service.calculator.CrowdLevelAnalyzer;
import com.example.TripSpring.service.calculator.RouteMetricsCalculator;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
@Slf4j
@RequiredArgsConstructor
public class RouteOptimizer {
    private final TravelTimeCalculator travelTimeCalculator;
    private final CrowdLevelAnalyzer crowdLevelAnalyzer;
    private final RouteMetricsCalculator metricsCalculator;
    private final FirstMapService firstMapService;
    private final LocationInfoService locationInfoService;

    private static final int MIN_SCHEDULE_DURATION = 30; // 최소 일정 시간 (분)
    private static final int TRAVEL_BUFFER = 10; // 이동 시간 버퍼 (분)

    private TimeWindow createDefaultTimeWindow() {
        TimeWindow window = new TimeWindow();
        window.setStart(LocalDateTime.now().withHour(9).withMinute(0));
        window.setEnd(LocalDateTime.now().withHour(18).withMinute(0));
        return window;
    }

    public List<TimeWindow> findAvailableWindows(List<Schedule> schedules) {
        List<TimeWindow> windows = new ArrayList<>();
        if (schedules.isEmpty()) {
            windows.add(createDefaultTimeWindow());
            return windows;
        }

        // 일정들을 시작 시간 기준으로 정렬
        schedules.sort(Comparator.comparing(Schedule::getStartTime));

        // 첫 일정 전 시간대 확인
        Schedule firstSchedule = schedules.get(0);
        if (firstSchedule.getStartTime().isAfter(LocalDateTime.now().plusHours(1))) {
            TimeWindow beforeFirst = new TimeWindow();
            beforeFirst.setStart(LocalDateTime.now());
            beforeFirst.setEnd(firstSchedule.getStartTime());
            windows.add(beforeFirst);
        }

        // 일정들 사이의 시간대 확인
        for (int i = 0; i < schedules.size() - 1; i++) {
            Schedule current = schedules.get(i);
            Schedule next = schedules.get(i + 1);

            // 이동시간을 고려한 여유 시간 계산
            int travelTime = travelTimeCalculator.calculateTravelTime(
                current.getLocation(),
                next.getLocation()
            );
            
            LocalDateTime earliestNextStart = current.getEndTime()
                .plusMinutes(travelTime)
                .plusMinutes(TRAVEL_BUFFER);

            if (earliestNextStart.plusMinutes(MIN_SCHEDULE_DURATION).isBefore(next.getStartTime())) {
                TimeWindow window = new TimeWindow();
                window.setStart(current.getEndTime());
                window.setEnd(next.getStartTime());
                window.setPreviousLocation(current.getLocation());
                window.setNextLocation(next.getLocation());
                windows.add(window);
            }
        }

        // 마지막 일정 후 시간대 확인
        Schedule lastSchedule = schedules.get(schedules.size() - 1);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(22).withMinute(0);
        
        if (lastSchedule.getEndTime().plusMinutes(MIN_SCHEDULE_DURATION).isBefore(endOfDay)) {
            TimeWindow afterLast = new TimeWindow();
            afterLast.setStart(lastSchedule.getEndTime());
            afterLast.setEnd(endOfDay);
            afterLast.setPreviousLocation(lastSchedule.getLocation());
            windows.add(afterLast);
        }

        return windows;
    }

    public EnhancedRouteResponse generateOptimalRoute(List<Schedule> fixedSchedules, List<Schedule> flexibleSchedules) {
        // 1. 기본 경로 생성
        Route basicRoute = generateBasicRoute(fixedSchedules, flexibleSchedules);
        return generateEnhancedResponse(basicRoute);
    }

    private Route generateBasicRoute(List<Schedule> fixedSchedules, List<Schedule> flexibleSchedules) {
        List<Schedule> allSchedules = new ArrayList<>(fixedSchedules);
        
        // 유연한 일정 우선순위 정렬
        flexibleSchedules.sort(Comparator.comparing(Schedule::getPriority));
        
        // 각 유연한 일정 최적 배치
        for (Schedule flexible : flexibleSchedules) {
            insertFlexibleSchedule(allSchedules, flexible);
        }

        Route route = new Route();
        route.setSchedules(allSchedules);
        
        // 실제 이동 거리와 시간 계산
        double totalDistance = metricsCalculator.calculateMetrics(allSchedules).getTotalDistance();
        route.setTotalDistance(totalDistance);
        route.setTotalTime(metricsCalculator.calculateMetrics(allSchedules).getTotalTravelTime());
        route.setTotalCost(metricsCalculator.calculateMetrics(allSchedules).getTotalCost());

        return route;
    }

    private void insertFlexibleSchedule(List<Schedule> existingSchedules, Schedule flexible) {
        List<TimeWindow> availableWindows = findAvailableWindows(existingSchedules);
        Optional<TimeWindow> bestWindow = findBestTimeWindow(availableWindows, flexible);

        if (bestWindow.isPresent()) {
            TimeWindow window = bestWindow.get();
            setScheduleTimes(flexible, window);
            insertInOrder(existingSchedules, flexible);
            log.debug("Scheduled {} from {} to {}", flexible.getName(), 
                     flexible.getStartTime(), flexible.getEndTime());
        } else {
            log.warn("Could not find suitable time window for: {}", flexible.getName());
        }
    }

    private void setScheduleTimes(Schedule schedule, TimeWindow window) {
        LocalDateTime start = window.getStart();
        if (window.getPreviousLocation() != null) {
            int travelTime = travelTimeCalculator.calculateTravelTime(
                window.getPreviousLocation(), 
                schedule.getLocation()
            );
            start = start.plusMinutes(travelTime);
        }
        
        schedule.setStartTime(start);
        schedule.setEndTime(start.plusMinutes(MIN_SCHEDULE_DURATION));
    }

    private void insertInOrder(List<Schedule> schedules, Schedule newSchedule) {
        int insertIndex = schedules.size();
        for (int i = 0; i < schedules.size(); i++) {
            if (schedules.get(i).getStartTime().isAfter(newSchedule.getStartTime())) {
                insertIndex = i;
                break;
            }
        }
        schedules.add(insertIndex, newSchedule);
    }

    private Optional<TimeWindow> findBestTimeWindow(List<TimeWindow> windows, Schedule schedule) {
        return windows.stream()
            .filter(window -> isWindowSuitable(window, schedule))
            .min(Comparator.comparing(window -> calculateWindowCost(window, schedule)));
    }

    private boolean isWindowSuitable(TimeWindow window, Schedule schedule) {
        // 최소 필요 시간 계산
        int requiredMinutes = MIN_SCHEDULE_DURATION + TRAVEL_BUFFER;
        
        // 이전 위치에서의 이동 시간 고려
        if (window.getPreviousLocation() != null) {
            requiredMinutes += travelTimeCalculator.calculateTravelTime(
                window.getPreviousLocation(),
                schedule.getLocation()
            );
        }
        
        // 다음 위치로의 이동 시간 고려
        if (window.getNextLocation() != null) {
            requiredMinutes += travelTimeCalculator.calculateTravelTime(
                schedule.getLocation(),
                window.getNextLocation()
            );
        }
        
        // 시간 윈도우 크기가 충분한지 확인
        Duration windowDuration = Duration.between(window.getStart(), window.getEnd());
        return windowDuration.toMinutes() >= requiredMinutes;
    }

    private double calculateWindowCost(TimeWindow window, Schedule schedule) {
        double cost = 0.0;
        LocalDateTime time = window.getStart();
        
        // 혼잡도 비용
        cost += crowdLevelAnalyzer.analyzeCrowdLevel(time) * 2.0;
        
        // 이동 시간 비용
        if (window.getPreviousLocation() != null) {
            int travelTime = travelTimeCalculator.calculateTravelTime(
                window.getPreviousLocation(),
                schedule.getLocation()
            );
            cost += travelTime * 0.1;
        }
        
        return cost;
    }

    private EnhancedRouteResponse generateEnhancedResponse(Route basicRoute) {
        EnhancedRouteResponse response = new EnhancedRouteResponse();
        List<Schedule> schedules = basicRoute.getSchedules();

        response.setSegments(generateTripSegments(schedules));
        response.setSchedules(generateEnhancedSchedules(schedules));
        response.setTimeSlotAnalyses(generateTimeSlotAnalyses(schedules));
        response.setOptimizationReasons(generateOptimizationReasons(schedules));
        response.setMetrics(metricsCalculator.calculateMetrics(schedules));

        return response;
    }

    private List<TripSegment> generateTripSegments(List<Schedule> schedules) {
        List<TripSegment> segments = new ArrayList<>();
        
        for (int i = 0; i < schedules.size() - 1; i++) {
            Schedule current = schedules.get(i);
            Schedule next = schedules.get(i + 1);
            
            int travelTime = travelTimeCalculator.calculateTravelTime(
                current.getLocation(),
                next.getLocation()
            );
            
            TripSegment segment = new TripSegment();
            segment.setFromLocation(current.getName());
            segment.setToLocation(next.getName());
            segment.setDepartureTime(current.getEndTime());
            segment.setArrivalTime(travelTimeCalculator.calculateArrivalTime(
                current.getEndTime(),
                current.getLocation(),
                next.getLocation()
            ));
            
            TrafficInfo trafficInfo = firstMapService.getTrafficInfo(
                current.getLocation(),
                next.getLocation()
            );
            
            segment.setDistance(trafficInfo.getDistance());
            segment.setEstimatedTime(travelTime);
            segment.setTrafficRate(trafficInfo.getTrafficRate());
            segment.setReason(generateSegmentReason(current, next, trafficInfo));
            
            segments.add(segment);
        }
        
        return segments;
    }

    private List<EnhancedSchedule> generateEnhancedSchedules(List<Schedule> schedules) {
        List<EnhancedSchedule> enhancedSchedules = new ArrayList<>();
        
        for (Schedule schedule : schedules) {
            EnhancedSchedule enhanced = new EnhancedSchedule();
            enhanced.setSchedule(schedule);
            
            PlaceInfo placeInfo = locationInfoService.getPlaceInfo(schedule.getName());
            PlaceDetailsInfo details = new PlaceDetailsInfo();
            
            details.setCurrentCrowdLevel(crowdLevelAnalyzer.analyzeCrowdLevel(schedule.getStartTime()));
            details.setOptimalVisitTime(determineOptimalVisitTime(placeInfo));
            
            enhanced.setPlaceDetails(details);
            enhanced.setSchedulingReason(generateSchedulingReason(schedule, placeInfo));
            
            enhancedSchedules.add(enhanced);
        }
        
        return enhancedSchedules;
    }

    private List<TimeSlotAnalysis> generateTimeSlotAnalyses(List<Schedule> schedules) {
        List<TimeSlotAnalysis> analyses = new ArrayList<>();
        
        for (Schedule schedule : schedules) {
            TimeSlotAnalysis analysis = new TimeSlotAnalysis();
            analysis.setStartTime(schedule.getStartTime());
            analysis.setEndTime(schedule.getEndTime());
            analysis.setCrowdedness(crowdLevelAnalyzer.analyzeCrowdLevel(schedule.getStartTime()));
            analysis.setTrafficCondition(determineTrafficCondition(schedule));
            analysis.setRushHour(crowdLevelAnalyzer.isRushHour(schedule.getStartTime().toLocalTime()));
            analysis.setOptimalTime(isOptimalTime(schedule));
            analysis.setConsiderations(generateConsiderations(schedule));
            
            analyses.add(analysis);
        }
        
        return analyses;
    }

    private List<String> generateOptimizationReasons(List<Schedule> schedules) {
        List<String> reasons = new ArrayList<>();
        
        for (int i = 0; i < schedules.size() - 1; i++) {
            Schedule current = schedules.get(i);
            Schedule next = schedules.get(i + 1);
            
            int travelTime = travelTimeCalculator.calculateTravelTime(
                current.getLocation(),
                next.getLocation()
            );
            
            String reason = String.format("%s-%s 구간: 실제 이동시간 %d분 예상, %s",
                current.getName(),
                next.getName(),
                travelTime,
                determineTrafficCondition(next)
            );
            
            reasons.add(reason);
        }
        
        return reasons;
    }

    private String determineOptimalVisitTime(PlaceInfo placeInfo) {
        return placeInfo.getOpenTime().plusHours(1).toString(); // 오픈 1시간 후를 최적 시간으로 가정
    }

    private String generateSchedulingReason(Schedule schedule, PlaceInfo placeInfo) {
        StringBuilder reason = new StringBuilder();
        reason.append(schedule.getName())
              .append(" 방문 시간 선택 이유: ");

        if (crowdLevelAnalyzer.isRushHour(schedule.getStartTime().toLocalTime())) {
            reason.append("혼잡 시간대이나 다른 제약조건으로 인해 불가피한 선택");
        } else {
            reason.append("비교적 한산한 시간대 선택으로 효율적인 방문 가능");
        }

        return reason.toString();
    }

    private String determineTrafficCondition(Schedule schedule) {
        if (crowdLevelAnalyzer.isRushHour(schedule.getStartTime().toLocalTime())) {
            return "혼잡";
        } else if (crowdLevelAnalyzer.isLunchTime(schedule.getStartTime().toLocalTime())) {
            return "약간 혼잡";
        }
        return "원활";
    }

    private boolean isOptimalTime(Schedule schedule) {
        return !crowdLevelAnalyzer.isRushHour(schedule.getStartTime().toLocalTime()) &&
               !crowdLevelAnalyzer.isLunchTime(schedule.getStartTime().toLocalTime());
    }

    private List<String> generateConsiderations(Schedule schedule) {
        List<String> considerations = new ArrayList<>();
        double crowdLevel = crowdLevelAnalyzer.analyzeCrowdLevel(schedule.getStartTime());
        
        if (crowdLevelAnalyzer.isRushHour(schedule.getStartTime().toLocalTime())) {
            considerations.add("러시아워와 겹치는 시간대로, 이동시간 여유있게 계획 필요");
        }
        
        considerations.add(String.format("예상 혼잡도: %.1f", crowdLevel));
        considerations.add(String.format("예상 대기시간: %d분", (int)(crowdLevel * 20)));
        
        return considerations;
    }

    private String generateSegmentReason(Schedule from, Schedule to, TrafficInfo trafficInfo) {
        double crowd = crowdLevelAnalyzer.analyzeCrowdLevel(from.getEndTime());
        String condition = crowd < 0.5 ? "원활" : crowd < 0.8 ? "다소 혼잡" : "매우 혼잡";
        
        return String.format("%s에서 %s까지 이동: %s (혼잡도 %.1f)",
            from.getName(), to.getName(), condition, crowd);
    }
// RouteOptimizer.java에 추가할 메소드
    public Route generateOptimalRoute(Location currentLocation, List<Location> destinations) {
        // 현재 위치와 목적지들을 Schedule로 변환
        List<Schedule> fixedSchedules = new ArrayList<>();
        
        // 현재 위치를 시작점으로 하는 Schedule 생성
        Schedule currentSchedule = Schedule.create(
            LocalDateTime.now(),
            LocalDateTime.now().plusMinutes(MIN_SCHEDULE_DURATION),
            currentLocation,
            ScheduleType.FIXED,
            1,
            "Current Location"
        );
        fixedSchedules.add(currentSchedule);
        
        // 남은 목적지들을 Schedule로 변환
        List<Schedule> flexibleSchedules = destinations.stream()
            .map(location -> Schedule.create(
                null, // 시간은 최적화 과정에서 결정
                null,
                location,
                ScheduleType.FLEXIBLE,
                2,
                "Destination"
            ))
            .collect(Collectors.toList());
        
        // 기존 메소드를 활용하여 경로 생성
        Route basicRoute = generateBasicRoute(fixedSchedules, flexibleSchedules);
        return basicRoute;
    }
}