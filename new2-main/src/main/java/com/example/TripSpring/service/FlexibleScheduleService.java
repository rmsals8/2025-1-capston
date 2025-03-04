package com.example.TripSpring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.example.TripSpring.dto.domain.FlexiblePlaceOption;
import com.example.TripSpring.domain.PlaceInfo;
import com.example.TripSpring.domain.Schedule;
import com.example.TripSpring.domain.TimeWindow;
import com.example.TripSpring.dto.response.FlexibleScheduleResponse;
import com.example.TripSpring.dto.response.FlexibleScheduleResponse.RouteOption;
import com.example.TripSpring.dto.response.FlexibleScheduleResponse.ScheduleItem;
import com.example.TripSpring.dto.response.FlexibleScheduleResponse.RouteSegment;
import com.example.TripSpring.dto.response.NearbyPlacesResponse;
import com.example.TripSpring.exception.OptimizationException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlexibleScheduleService {
    
    private final NearbyPlaceService nearbyPlaceService;
    private final RouteCalculatorService routeCalculatorService;
    
    private static final int MAX_ROUTE_OPTIONS = 5; // 최대 경로 옵션 수
    private static final int MIN_TRAVEL_TIME = 15; // 최소 이동 시간(분)
    private static final double NEARBY_RADIUS = 1000.0; // 주변 검색 반경(m)
    
    public FlexibleScheduleResponse optimizeFlexibleSchedules(
            List<Schedule> fixedSchedules, 
            List<FlexiblePlaceOption> flexibleOptions) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 고정 일정을 시간순으로 정렬
            fixedSchedules.sort(Comparator.comparing(Schedule::getStartTime));
            
            // 2. 유연한 일정 옵션 우선순위 정렬
            flexibleOptions.sort(Comparator.comparing(FlexiblePlaceOption::getPriority));
            
            // 3. 가용 시간 슬롯 찾기
            List<TimeWindow> availableWindows = findAvailableTimeWindows(fixedSchedules);
            
            log.info("Found {} available time windows between fixed schedules", availableWindows.size());
            
            // 4. 주변 장소 검색
            Map<FlexiblePlaceOption, List<PlaceInfo>> placesByOption = findNearbyPlacesForOptions(
                    flexibleOptions, fixedSchedules);
            
            // 5. 가능한 일정 조합 생성
            List<List<ScheduleItem>> possibleCombinations = generateCombinations(
                    fixedSchedules, flexibleOptions, placesByOption, availableWindows);
            
            log.info("Generated {} possible schedule combinations", possibleCombinations.size());
            
            // 6. 조합 평가 및 최적 경로 선택
            List<RouteOption> routeOptions = evaluateAndRankCombinations(possibleCombinations);
            
            // 7. 응답 생성
            return FlexibleScheduleResponse.builder()
                    .routeOptions(routeOptions)
                    .metrics(FlexibleScheduleResponse.OptimizationMetrics.builder()
                            .processedCombinations(possibleCombinations.size())
                            .filteredOptions(possibleCombinations.size() - routeOptions.size())
                            .processingTimeMs(System.currentTimeMillis() - startTime)
                            .algorithm("Priority-based Greedy Optimization")
                            .build())
                    .build();
            
        } catch (Exception e) {
            log.error("Failed to optimize flexible schedules", e);
            throw new OptimizationException("Failed to optimize schedules: " + e.getMessage(), e);
        }
    }
    
    private List<TimeWindow> findAvailableTimeWindows(List<Schedule> fixedSchedules) {
        List<TimeWindow> windows = new ArrayList<>();
        
        if (fixedSchedules.isEmpty()) {
            // 고정 일정이 없는 경우 기본 시간대 반환
            TimeWindow defaultWindow = new TimeWindow();
            defaultWindow.setStart(LocalDateTime.now().withHour(9).withMinute(0));
            defaultWindow.setEnd(LocalDateTime.now().withHour(21).withMinute(0));
            windows.add(defaultWindow);
            return windows;
        }
        
        // 첫 일정 전 시간대
        Schedule firstSchedule = fixedSchedules.get(0);
        LocalDateTime now = LocalDateTime.now();
        
        if (firstSchedule.getStartTime().isAfter(now.plusMinutes(30))) {
            TimeWindow beforeFirst = new TimeWindow();
            beforeFirst.setStart(now);
            beforeFirst.setEnd(firstSchedule.getStartTime());
            windows.add(beforeFirst);
        }
        
        // 일정 사이 시간대
        for (int i = 0; i < fixedSchedules.size() - 1; i++) {
            Schedule current = fixedSchedules.get(i);
            Schedule next = fixedSchedules.get(i + 1);
            
            LocalDateTime earliestNextStart = current.getEndTime().plusMinutes(MIN_TRAVEL_TIME);
            
            if (earliestNextStart.isBefore(next.getStartTime())) {
                TimeWindow between = new TimeWindow();
                between.setStart(current.getEndTime());
                between.setEnd(next.getStartTime());
                between.setPreviousSchedule(current);
                between.setNextSchedule(next);
                windows.add(between);
            }
        }
        
        // 마지막 일정 후 시간대
        Schedule lastSchedule = fixedSchedules.get(fixedSchedules.size() - 1);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(22).withMinute(0);
        
        if (lastSchedule.getEndTime().plusMinutes(30).isBefore(endOfDay)) {
            TimeWindow afterLast = new TimeWindow();
            afterLast.setStart(lastSchedule.getEndTime());
            afterLast.setEnd(endOfDay);
            afterLast.setPreviousSchedule(lastSchedule);
            windows.add(afterLast);
        }
        
        return windows;
    }
    
    private Map<FlexiblePlaceOption, List<PlaceInfo>> findNearbyPlacesForOptions(
            List<FlexiblePlaceOption> options, List<Schedule> fixedSchedules) {
        
        Map<FlexiblePlaceOption, List<PlaceInfo>> results = new HashMap<>();
        
        for (FlexiblePlaceOption option : options) {
            List<PlaceInfo> allPlaces = new ArrayList<>();
            
            // 각 고정 일정 주변의 장소 검색
            for (Schedule fixed : fixedSchedules) {
                NearbyPlacesResponse response = nearbyPlaceService.findNearbyPlaces(
                        fixed.getLatitude(),
                        fixed.getLongitude(),
                        option.getType(),
                        NEARBY_RADIUS
                );
                
                allPlaces.addAll(response.getPlaces());
            }
            
            // 중복 제거 (같은 장소가 여러 고정 일정 근처에 있을 수 있음)
            List<PlaceInfo> uniquePlaces = allPlaces.stream()
                    .collect(Collectors.toMap(
                            PlaceInfo::getId,
                            place -> place,
                            (place1, place2) -> place1
                    ))
                    .values()
                    .stream()
                    .collect(Collectors.toList());
            
            results.put(option, uniquePlaces);
            log.info("Found {} unique {} places near fixed schedules", 
                    uniquePlaces.size(), option.getType());
        }
        
        return results;
    }
    
    private List<List<ScheduleItem>> generateCombinations(
            List<Schedule> fixedSchedules,
            List<FlexiblePlaceOption> flexibleOptions,
            Map<FlexiblePlaceOption, List<PlaceInfo>> placesByOption,
            List<TimeWindow> availableWindows) {
        
        // 고정 일정을 ScheduleItem으로 변환
        List<ScheduleItem> fixedItems = fixedSchedules.stream()
                .map(this::convertFixedSchedule)
                .collect(Collectors.toList());
        
        // 각 유연한 일정 옵션에 대한 가능한 장소와 시간 조합 생성
        List<List<ScheduleItem>> allCombinations = new ArrayList<>();
        allCombinations.add(new ArrayList<>(fixedItems)); // 초기 조합은 고정 일정만 포함
        
        // 각 유연한 일정에 대해 조합 확장
        for (FlexiblePlaceOption option : flexibleOptions) {
            List<PlaceInfo> availablePlaces = placesByOption.get(option);
            
            if (availablePlaces == null || availablePlaces.isEmpty()) {
                log.warn("No places found for option: {}", option.getName());
                continue;
            }
            
            List<List<ScheduleItem>> newCombinations = new ArrayList<>();
            
            // 현재까지의 각 조합에 대해
            for (List<ScheduleItem> currentCombination : allCombinations) {
                // 각 가능한 장소에 대해
                for (PlaceInfo place : availablePlaces) {
                    // 가능한 시간 슬롯 찾기
                    List<TimeSlot> possibleTimeSlots = findPossibleTimeSlots(
                            currentCombination, place, option.getDuration(), availableWindows);
                    
                    if (possibleTimeSlots.isEmpty()) {
                        continue; // 이 장소는 시간 제약으로 인해 불가능
                    }
                    
                    // 첫 번째 가능한 시간 슬롯 사용 (더 정교한 알고리즘은 모든 슬롯을 고려할 수 있음)
                    TimeSlot bestSlot = possibleTimeSlots.get(0);
                    
                    // 새 일정 아이템 생성
                    ScheduleItem newItem = ScheduleItem.builder()
                            .id(UUID.randomUUID().toString())
                            .name(place.getName())
                            .location(place.getAddress())
                            .startTime(bestSlot.getStart())
                            .endTime(bestSlot.getEnd())
                            .type("FLEXIBLE")
                            .latitude(place.getLatitude())
                            .longitude(place.getLongitude())
                            .duration(option.getDuration())
                            .isOptimized(true)
                            .placeType(option.getType())
                            .build();
                    
                    // 새 조합 생성 (기존 일정 + 새 일정)
                    List<ScheduleItem> newCombination = new ArrayList<>(currentCombination);
                    newCombination.add(newItem);
                    
                    // 시간순 정렬
                    newCombination.sort(Comparator.comparing(ScheduleItem::getStartTime));
                    
                    newCombinations.add(newCombination);
                }
            }
            
            // 조합 수가 너무 많아지는 것을 방지
            if (!newCombinations.isEmpty()) {
                // 이전 단계의 조합은 더 이상 고려하지 않음
                allCombinations = newCombinations;
                
                // 조합 수 제한
                if (allCombinations.size() > 100) {
                    // 간단한 휴리스틱: 일정 간 시간 간격이 균등한 조합 우선
                    allCombinations.sort(Comparator.comparing(this::calculateTimeBalanceScore));
                    allCombinations = allCombinations.subList(0, 100);
                }
            }
        }
        
        return allCombinations;
    }
    
    private List<TimeSlot> findPossibleTimeSlots(
            List<ScheduleItem> currentSchedules,
            PlaceInfo place,
            int duration,
            List<TimeWindow> availableWindows) {
        
        List<TimeSlot> possibleSlots = new ArrayList<>();
        
        // 현재 일정을 시간순으로 정렬
        currentSchedules.sort(Comparator.comparing(ScheduleItem::getStartTime));
        
        // 각 가용 시간 윈도우 확인
        for (TimeWindow window : availableWindows) {
            // 이미 다른 일정이 이 윈도우를 차지하고 있는지 확인
            boolean isOccupied = false;
            for (ScheduleItem item : currentSchedules) {
                // 일정이 윈도우와 겹치는지 확인
                if (!(item.getEndTime().isBefore(window.getStart()) || 
                      item.getStartTime().isAfter(window.getEnd()))) {
                    isOccupied = true;
                    break;
                }
            }
            
            if (isOccupied) {
                continue;
            }
            
            // 이 윈도우에서 가능한 시간 슬롯 찾기
            LocalDateTime earliestStart = window.getStart();
            LocalDateTime latestEnd = window.getEnd();
            
            // 이전 일정에서의 이동 시간 고려
            Schedule previousSchedule = window.getPreviousSchedule();
            if (previousSchedule != null) {
                int travelTime = estimateTravelTime(
                        previousSchedule.getLatitude(),
                        previousSchedule.getLongitude(),
                        place.getLatitude(),
                        place.getLongitude()
                );
                earliestStart = earliestStart.plusMinutes(travelTime);
            }
            
            // 다음 일정까지의 이동 시간 고려
            Schedule nextSchedule = window.getNextSchedule();
            if (nextSchedule != null) {
                int travelTime = estimateTravelTime(
                        place.getLatitude(),
                        place.getLongitude(),
                        nextSchedule.getLatitude(),
                        nextSchedule.getLongitude()
                );
                latestEnd = latestEnd.minusMinutes(travelTime);
            }
            
            // 일정 자체의 소요 시간 고려
            if (Duration.between(earliestStart, latestEnd).toMinutes() >= duration) {
                // 가장 이른 시작 시간에 배치
                possibleSlots.add(new TimeSlot(
                        earliestStart,
                        earliestStart.plusMinutes(duration)
                ));
                
                // 가장 늦은 시작 시간에 배치 (다양성을 위해)
                LocalDateTime lateStart = latestEnd.minusMinutes(duration);
                if (!lateStart.equals(earliestStart)) {
                    possibleSlots.add(new TimeSlot(
                            lateStart,
                            latestEnd
                    ));
                }
                
                // 중간 시간에 배치 (다양성을 위해)
                LocalDateTime middleStart = earliestStart.plus(
                        Duration.between(earliestStart, lateStart).dividedBy(2)
                );
                if (!middleStart.equals(earliestStart) && !middleStart.equals(lateStart)) {
                    possibleSlots.add(new TimeSlot(
                            middleStart,
                            middleStart.plusMinutes(duration)
                    ));
                }
            }
        }
        
        return possibleSlots;
    }
    
    private int estimateTravelTime(
            double fromLat, double fromLon, double toLat, double toLon) {
        // 실제 구현에서는 라우팅 서비스를 통해 계산
        // 여기서는 단순 거리 기반 추정
        double distance = calculateDistance(fromLat, fromLon, toLat, toLon);
        
        // 대략적인 이동 시간 계산 (평균 시속 30km로 가정)
        int travelTimeMinutes = (int) Math.ceil(distance / 30.0 * 60);
        
        // 최소 이동 시간 보장
        return Math.max(travelTimeMinutes, MIN_TRAVEL_TIME);
    }
    
    private double calculateDistance(
            double lat1, double lon1, double lat2, double lon2) {
        // 하버사인 공식을 사용한 거리 계산
        double earthRadius = 6371; // 지구 반경(km)
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return earthRadius * c; // 킬로미터 단위 거리
    }
    
    private double calculateTimeBalanceScore(List<ScheduleItem> schedules) {
        // 일정 간 시간 간격의 표준 편차 계산 (낮을수록 균등)
        if (schedules.size() < 3) {
            return 0.0; // 일정이 2개 이하면 간격을 계산할 수 없음
        }
        
        List<Integer> gaps = new ArrayList<>();
        for (int i = 0; i < schedules.size() - 1; i++) {
            ScheduleItem current = schedules.get(i);
            ScheduleItem next = schedules.get(i + 1);
            
            int gapMinutes = (int) Duration.between(
                    current.getEndTime(), next.getStartTime()).toMinutes();
            
            gaps.add(gapMinutes);
        }
        
        // 평균 간격
        double avgGap = gaps.stream().mapToInt(Integer::intValue).average().orElse(0);
        
        // 표준 편차 계산
        double sumSquaredDiff = gaps.stream()
                .mapToDouble(gap -> Math.pow(gap - avgGap, 2))
                .sum();
        
        double stdDev = Math.sqrt(sumSquaredDiff / gaps.size());
        
        // 반환값은 편차의 역수 (편차가 낮을수록 점수 높음)
        return stdDev == 0 ? Double.MAX_VALUE : 1.0 / stdDev;
    }
    
    private List<RouteOption> evaluateAndRankCombinations(List<List<ScheduleItem>> combinations) {
        if (combinations.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<RouteOption> routeOptions = new ArrayList<>();
        
        for (List<ScheduleItem> combination : combinations) {
            // 각 조합에 대한 경로 세그먼트 생성
            List<RouteSegment> segments = calculateRouteSegments(combination);
            
            // 총 거리, 시간, 비용 계산
            double totalDistance = segments.stream()
                    .mapToDouble(RouteSegment::getDistance)
                    .sum();
            
            int totalDuration = segments.stream()
                    .mapToInt(RouteSegment::getDuration)
                    .sum();
            
            double totalCost = segments.stream()
                    .mapToDouble(segment -> {
                        // 교통 수단별 비용 계산
                        if ("TAXI".equals(segment.getTransportMode())) {
                            return 3800 + segment.getDistance() * 1000; // 기본요금 + km당 1000원
                        } else if ("PUBLIC".equals(segment.getTransportMode())) {
                            return 1250; // 기본 대중교통 요금
                        }
                        return 0.0; // 도보는 무료
                    })
                    .sum();
            
            // 최적화 점수 계산
            Map<String, Double> scores = calculateOptimizationScores(combination, segments);
            
            // 경로 옵션 생성
            RouteOption option = RouteOption.builder()
                    .id(UUID.randomUUID().toString())
                    .schedules(combination)
                    .segments(segments)
                    .totalDistance(totalDistance)
                    .totalDuration(totalDuration)
                    .totalCost(totalCost)
                    .scores(scores)
                    .build();
            
            routeOptions.add(option);
        }
        
        // 점수 기반 정렬
        routeOptions.sort((o1, o2) -> {
            // 총 점수 계산 (점수의 가중 평균)
            double score1 = calculateOverallScore(o1.getScores());
            double score2 = calculateOverallScore(o2.getScores());
            return Double.compare(score2, score1); // 높은 점수 우선
        });
        
        // 최대 N개의 경로 옵션 선택
        return routeOptions.stream()
                .limit(MAX_ROUTE_OPTIONS)
                .collect(Collectors.toList());
    }
    
    private List<RouteSegment> calculateRouteSegments(List<ScheduleItem> schedules) {
        List<RouteSegment> segments = new ArrayList<>();
        
        for (int i = 0; i < schedules.size() - 1; i++) {
            ScheduleItem current = schedules.get(i);
            ScheduleItem next = schedules.get(i + 1);
            
            // 경로 세그먼트 계산
            double distance = calculateDistance(
                    current.getLatitude(), current.getLongitude(),
                    next.getLatitude(), next.getLongitude());
            
            // 교통 모드 선택 (간단한 구현: 2km 이내는 도보, 이외에는 택시)
            String transportMode = distance <= 2.0 ? "WALK" : "TAXI";
            
            // 이동 시간 계산
            int duration;
            if ("WALK".equals(transportMode)) {
                duration = (int) Math.ceil(distance / 4.0 * 60); // 시속 4km 가정
            } else {
                duration = (int) Math.ceil(distance / 30.0 * 60); // 시속 30km 가정
            }
            
            // 교통 혼잡도 계산
            double trafficRate = estimateTrafficRate(
                    current.getEndTime().toLocalTime().getHour());
            
            RouteSegment segment = RouteSegment.builder()
                    .from(current.getName())
                    .to(next.getName())
                    .distance(distance)
                    .duration(duration)
                    .trafficRate(trafficRate)
                    .transportMode(transportMode)
                    .build();
            
            segments.add(segment);
        }
        
        return segments;
    }
    
    private double estimateTrafficRate(int hour) {
        // 시간대별 교통 혼잡도 추정
        if (hour >= 7 && hour <= 9) {
            return 0.8; // 아침 러시아워
        } else if (hour >= 17 && hour <= 19) {
            return 0.9; // 저녁 러시아워
        } else if (hour >= 12 && hour <= 14) {
            return 0.6; // 점심 시간
        } else if (hour >= 22 || hour <= 5) {
            return 0.2; // 심야/새벽
        } else {
            return 0.4; // 그 외 시간대
        }
    }
    
    private Map<String, Double> calculateOptimizationScores(
            List<ScheduleItem> schedules, List<RouteSegment> segments) {
        Map<String, Double> scores = new HashMap<>();
        
        // 1. 이동 효율성 점수 (이동 거리/시간 대비 방문 장소 수)
        double totalDistance = segments.stream().mapToDouble(RouteSegment::getDistance).sum();
        double distanceEfficiency = schedules.size() / Math.max(totalDistance, 0.1);
        scores.put("moveEfficiency", normalizeScore(distanceEfficiency, 0.2, 2.0));
        
        // 2. 시간 균형 점수 (일정 간 간격의 균일성)
        scores.put("timeBalance", calculateTimeBalanceScore(schedules));
        
        // 3. 교통 점수 (교통 혼잡도의 역수)
        double avgTrafficRate = segments.stream()
                .mapToDouble(RouteSegment::getTrafficRate)
                .average()
                .orElse(0.5);
        scores.put("traffic", 1.0 - avgTrafficRate);
        
        // 4. 다양성 점수 (서로 다른 유형의 장소 방문)
        long uniqueTypes = schedules.stream()
                .filter(s -> "FLEXIBLE".equals(s.getType()))
                .map(ScheduleItem::getPlaceType)
                .distinct()
                .count();
        scores.put("diversity", normalizeScore(uniqueTypes, 1, 5));
        
        return scores;
    }
    
    private double normalizeScore(double value, double min, double max) {
        return Math.min(1.0, Math.max(0.0, (value - min) / (max - min)));
    }
    
    private double calculateOverallScore(Map<String, Double> scores) {
        double moveEfficiency = scores.getOrDefault("moveEfficiency", 0.0) * 0.3;
        double timeBalance = scores.getOrDefault("timeBalance", 0.0) * 0.3;
        double traffic = scores.getOrDefault("traffic", 0.0) * 0.25;
        double diversity = scores.getOrDefault("diversity", 0.0) * 0.15;
        
        return moveEfficiency + timeBalance + traffic + diversity;
    }
    
    private ScheduleItem convertFixedSchedule(Schedule schedule) {
        return ScheduleItem.builder()
                .id(schedule.getId())
                .name(schedule.getName())
                .location(schedule.getLocation())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .type("FIXED")
                .latitude(schedule.getLatitude())
                .longitude(schedule.getLongitude())
                .duration((int) Duration.between(schedule.getStartTime(), schedule.getEndTime()).toMinutes())
                .isOptimized(false)
                .build();
    }
    
    @lombok.Value
    private static class TimeSlot {
        LocalDateTime start;
        LocalDateTime end;
    }
}
