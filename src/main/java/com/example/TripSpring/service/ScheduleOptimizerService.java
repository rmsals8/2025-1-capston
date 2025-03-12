package com.example.TripSpring.service;

import lombok.RequiredArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleOptimizerService {
    private final PlaceAnalyzerService placeAnalyzer;
    private final RouteAnalyzerService routeAnalyzer;

    // 가중치 상수 정의
    private static final double WEIGHT_OPERATING_HOURS = 0.3;  // 영업시간 가중치
    private static final double WEIGHT_PEAK_TIME = 0.25;      // 피크타임 가중치
    private static final double WEIGHT_TRAFFIC = 0.25;        // 교통 상황 가중치
    private static final double WEIGHT_TIME_BALANCE = 0.2;    // 시간 균형 가중치

    /**
     * 최적화 입력 데이터 클래스
     */
    @Getter
    public static class OptimizationRequest {
        private final String placeName;
        private final double latitude;
        private final double longitude;
        private final LocalDateTime earliestStart;
        private final LocalDateTime latestEnd;
        private final List<ScheduleSlot> existingSchedules;
        private final int desiredDuration;  // 희망 체류 시간(분)

        public OptimizationRequest(String placeName, double latitude, double longitude,
                                 LocalDateTime earliestStart, LocalDateTime latestEnd,
                                 List<ScheduleSlot> existingSchedules, int desiredDuration) {
            this.placeName = placeName;
            this.latitude = latitude;
            this.longitude = longitude;
            this.earliestStart = earliestStart;
            this.latestEnd = latestEnd;
            this.existingSchedules = existingSchedules;
            this.desiredDuration = desiredDuration;
        }
    }

    /**
     * 기존 일정 정보 클래스
     */
    @Getter
    public static class ScheduleSlot {
        private final String placeName;
        private final double latitude;
        private final double longitude;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;

        public ScheduleSlot(String placeName, double latitude, double longitude,
                          LocalDateTime startTime, LocalDateTime endTime) {
            this.placeName = placeName;
            this.latitude = latitude;
            this.longitude = longitude;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    /**
     * 최적화 결과 클래스
     */
    @Getter
    public static class OptimizationResult {
        private final LocalDateTime optimalStartTime;
        private final LocalDateTime optimalEndTime;
        private final double score;
        private final Map<String, Double> componentScores;
        private final List<RouteAnalyzerService.RouteSegment> routeSegments;
        private final String recommendation;

        public OptimizationResult(LocalDateTime optimalStartTime, LocalDateTime optimalEndTime,
                                double score, Map<String, Double> componentScores,
                                List<RouteAnalyzerService.RouteSegment> routeSegments,
                                String recommendation) {
            this.optimalStartTime = optimalStartTime;
            this.optimalEndTime = optimalEndTime;
            this.score = score;
            this.componentScores = componentScores;
            this.routeSegments = routeSegments;
            this.recommendation = recommendation;
        }
    }

    /**
     * 시간대 평가 결과 클래스
     */
    private static class TimeSlotEvaluation {
        private final LocalDateTime startTime;
        private final double operatingHoursScore;
        private final double peakTimeScore;
        private final double trafficScore;
        private final double timeBalanceScore;
        private final double totalScore;
        private final List<RouteAnalyzerService.RouteSegment> routeSegments;

        public TimeSlotEvaluation(LocalDateTime startTime, double operatingHoursScore,
                                double peakTimeScore, double trafficScore, double timeBalanceScore,
                                List<RouteAnalyzerService.RouteSegment> routeSegments) {
            this.startTime = startTime;
            this.operatingHoursScore = operatingHoursScore;
            this.peakTimeScore = peakTimeScore;
            this.trafficScore = trafficScore;
            this.timeBalanceScore = timeBalanceScore;
            this.routeSegments = routeSegments;
            
            // 가중치를 적용한 총점 계산
            this.totalScore = (operatingHoursScore * WEIGHT_OPERATING_HOURS) +
                            (peakTimeScore * WEIGHT_PEAK_TIME) +
                            (trafficScore * WEIGHT_TRAFFIC) +
                            (timeBalanceScore * WEIGHT_TIME_BALANCE);
        }
    }

    /**
     * 최적 시간대 찾기
     */
    public OptimizationResult findOptimalTimeSlot(OptimizationRequest request) {
        try {
            // 1. 장소 정보 분석
            PlaceAnalyzerService.PlaceAnalysis placeAnalysis = 
                placeAnalyzer.analyzePlaceDetails(request.getPlaceName(), 
                                                request.getLatitude(), 
                                                request.getLongitude());
            
            if (placeAnalysis == null) {
                log.error("Failed to analyze place: {}", request.getPlaceName());
                return null;
            }

            // 2. 가능한 시간대 생성 (30분 단위)
            List<LocalDateTime> possibleTimes = generatePossibleTimeSlots(
                request.getEarliestStart(),
                request.getLatestEnd(),
                30
            );

            // 3. 각 시간대 평가
            List<TimeSlotEvaluation> evaluations = evaluateTimeSlots(
                possibleTimes,
                request,
                placeAnalysis
            );

            // 4. 최적 시간대 선정
            TimeSlotEvaluation bestSlot = findBestTimeSlot(evaluations);
            
            if (bestSlot == null) {
                log.error("No suitable time slot found");
                return null;
            }

            // 5. 결과 생성
            return createOptimizationResult(bestSlot, request);

        } catch (Exception e) {
            log.error("Error finding optimal time slot", e);
            return null;
        }
    }

    /**
     * 가능한 시간대 생성
     */
    private List<LocalDateTime> generatePossibleTimeSlots(
        LocalDateTime start,
        LocalDateTime end,
        int intervalMinutes
    ) {
        List<LocalDateTime> slots = new ArrayList<>();
        LocalDateTime current = start;
        
        while (current.plusMinutes(intervalMinutes).isBefore(end)) {
            slots.add(current);
            current = current.plusMinutes(intervalMinutes);
        }
        
        return slots;
    }

    /**
     * 시간대 평가 수행
     */
    private List<TimeSlotEvaluation> evaluateTimeSlots(
        List<LocalDateTime> slots,
        OptimizationRequest request,
        PlaceAnalyzerService.PlaceAnalysis placeAnalysis
    ) {
        List<TimeSlotEvaluation> evaluations = new ArrayList<>();

        for (LocalDateTime slot : slots) {
            // 1. 영업시간 점수 계산
            double operatingHoursScore = evaluateOperatingHours(
                slot,
                request.getDesiredDuration(),
                placeAnalysis
            );

            // 2. 피크타임 점수 계산
            double peakTimeScore = evaluatePeakTime(
                slot,
                placeAnalysis.peakHours()
            );

            // 3. 교통 상황 점수 계산
            RouteAnalyzerService.RouteAnalysis routeAnalysis = null;
            double trafficScore = 0.0;
            List<RouteAnalyzerService.RouteSegment> routeSegments = new ArrayList<>();

            if (!request.getExistingSchedules().isEmpty()) {
                ScheduleSlot previousSchedule = findPreviousSchedule(slot, request.getExistingSchedules());
                if (previousSchedule != null) {
                    routeAnalysis = routeAnalyzer.analyzeRoute(
                        previousSchedule.getLatitude(),
                        previousSchedule.getLongitude(),
                        request.getLatitude(),
                        request.getLongitude(),
                        slot
                    );

                    if (routeAnalysis != null) {
                        trafficScore = 1.0 - routeAnalysis.getCongestionLevel();
                        routeSegments = routeAnalysis.getSegments();
                    }
                }
            }

            // 4. 시간 균형 점수 계산
            double timeBalanceScore = evaluateTimeBalance(
                slot,
                request.getDesiredDuration(),
                request.getExistingSchedules()
            );

            // 5. 평가 결과 저장
            evaluations.add(new TimeSlotEvaluation(
                slot,
                operatingHoursScore,
                peakTimeScore,
                trafficScore,
                timeBalanceScore,
                routeSegments
            ));
        }

        return evaluations;
    }

    /**
     * 영업시간 평가
     */
    private double evaluateOperatingHours(
        LocalDateTime slot,
        int duration,
        PlaceAnalyzerService.PlaceAnalysis placeAnalysis
    ) {
        if (!placeAnalysis.isOpen()) {
            return 0.0;
        }

        LocalTime startTime = slot.toLocalTime();
        LocalTime endTime = slot.plusMinutes(duration).toLocalTime();
        LocalTime openTime = placeAnalysis.openTime();
        LocalTime closeTime = placeAnalysis.closeTime();

        if (startTime.isBefore(openTime) || endTime.isAfter(closeTime)) {
            return 0.0;
        }

        // 영업 시간 중간에 가까울수록 높은 점수
        LocalTime midPoint = startTime.plusMinutes(duration / 2);
        LocalTime businessMidPoint = openTime.plusSeconds(
            Duration.between(openTime, closeTime).getSeconds() / 2
        );

        Duration difference = Duration.between(midPoint, businessMidPoint).abs();
        Duration maxDifference = Duration.between(openTime, closeTime);

        return 1.0 - (difference.toMinutes() / (double) maxDifference.toMinutes());
    }

    /**
     * 피크타임 평가
     */
    private double evaluatePeakTime(
        LocalDateTime slot,
        List<String> peakHours
    ) {
        LocalTime time = slot.toLocalTime();
        
        // 피크타임과의 거리가 멀수록 높은 점수
        double minDistance = peakHours.stream()
            .map(peak -> {
                LocalTime peakTime = LocalTime.parse(peak);
                return Duration.between(time, peakTime).abs().toMinutes();
            })
            .min(Long::compare)
            .orElse(720L); // 12시간 = 최대 거리

        return Math.min(minDistance / 180.0, 1.0); // 3시간 이상 차이나면 만점
    }

    /**
     * 시간 균형 평가
     */
    private double evaluateTimeBalance(
        LocalDateTime slot,
        int duration,
        List<ScheduleSlot> existingSchedules
    ) {
        if (existingSchedules.isEmpty()) {
            return 1.0;
        }

        List<Duration> gaps = new ArrayList<>();
        LocalDateTime proposedStart = slot;
        LocalDateTime proposedEnd = slot.plusMinutes(duration);

        // 이전 일정과의 간격
        ScheduleSlot previousSchedule = findPreviousSchedule(slot, existingSchedules);
        if (previousSchedule != null) {
            gaps.add(Duration.between(previousSchedule.getEndTime(), proposedStart));
        }

        // 다음 일정과의 간격
        ScheduleSlot nextSchedule = findNextSchedule(slot, existingSchedules);
        if (nextSchedule != null) {
            gaps.add(Duration.between(proposedEnd, nextSchedule.getStartTime()));
        }

        // 간격이 너무 좁거나 넓지 않은 경우 높은 점수
        return gaps.stream()
            .mapToDouble(gap -> {
                long minutes = gap.toMinutes();
                if (minutes < 30) return 0.0; // 30분 미만은 0점
                if (minutes > 180) return 0.5; // 3시간 초과는 중간점수
                return 1.0 - ((minutes - 30) / 150.0); // 30분~3시간 사이 점수
            })
            .average()
            .orElse(1.0);
    }

    /**
     * 이전 일정 찾기
     */
    private ScheduleSlot findPreviousSchedule(
        LocalDateTime reference,
        List<ScheduleSlot> schedules
    ) {
        return schedules.stream()
            .filter(s -> s.getEndTime().isBefore(reference))
            .max(Comparator.comparing(ScheduleSlot::getEndTime))
            .orElse(null);
    }

    /**
     * 다음 일정 찾기
     */
    private ScheduleSlot findNextSchedule(
        LocalDateTime reference,
        List<ScheduleSlot> schedules
    ) {
        return schedules.stream()
            .filter(s -> s.getStartTime().isAfter(reference))
            .min(Comparator.comparing(ScheduleSlot::getStartTime))
            .orElse(null);
    }

    /**
     * 최적 시간대 선택
     */
    private TimeSlotEvaluation findBestTimeSlot(List<TimeSlotEvaluation> evaluations) {
        return evaluations.stream()
            .max(Comparator.comparingDouble(e -> e.totalScore))
            .orElse(null);
    }

    /**
     * 최종 결과 생성
     */
private OptimizationResult createOptimizationResult(
        TimeSlotEvaluation bestSlot,
        OptimizationRequest request
    ) {
        // 구성 요소별 점수 맵 생성
        Map<String, Double> scores = new HashMap<>();
        scores.put("operatingHours", bestSlot.operatingHoursScore);
        scores.put("peakTime", bestSlot.peakTimeScore);
        scores.put("traffic", bestSlot.trafficScore);
        scores.put("timeBalance", bestSlot.timeBalanceScore);

        // 추천 사유 생성
        String recommendation = generateRecommendation(bestSlot, request);

        return new OptimizationResult(
            bestSlot.startTime,
            bestSlot.startTime.plusMinutes(request.getDesiredDuration()),
            bestSlot.totalScore,
            scores,
            bestSlot.routeSegments,
            recommendation
        );
    }

    /**
     * 추천 사유 생성
     */
    private String generateRecommendation(TimeSlotEvaluation slot, OptimizationRequest request) {
        StringBuilder reason = new StringBuilder();
        
        // 영업시간 관련
        if (slot.operatingHoursScore > 0.8) {
            reason.append("영업시간 중 최적의 방문 시간대, ");
        }

        // 피크타임 관련
        if (slot.peakTimeScore > 0.8) {
            reason.append("한산한 시간대, ");
        } else if (slot.peakTimeScore < 0.3) {
            reason.append("혼잡이 예상되는 시간대이나 다른 조건 고려, ");
        }

        // 교통 상황 관련
        if (slot.trafficScore > 0.8) {
            reason.append("교통 흐름이 원활, ");
        } else if (slot.trafficScore < 0.3) {
            reason.append("교통 혼잡 예상되나 불가피한 선택, ");
        }

        // 시간 균형 관련
        if (slot.timeBalanceScore > 0.8) {
            reason.append("전후 일정과 균형 잡힌 간격");
        }

        return reason.toString().replaceAll(", $", "");
    }
}