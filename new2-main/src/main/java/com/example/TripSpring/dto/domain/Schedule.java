package com.example.TripSpring.dto.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.Duration;
import com.example.TripSpring.dto.domain.route.TransportMode;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {
    private String id;
    private String name;                    // 장소 이름
    private Location location;              // 위치 정보
    private LocalDateTime startTime;        // 시작 시간
    private LocalDateTime endTime;          // 종료 시간
    private ScheduleType type;              // 고정/유동적 일정 구분
    private int priority;                   // 우선순위 (1-5)
    private PlaceCategory category;         // 장소 카테고리
    private int estimatedDuration;          // 예상 소요시간 (분)
    private double expectedCost;            // 예상 비용
    private VisitPreference visitPreference;// 방문 선호도 설정
    private String locationString; // 문자열 위치
    @Builder.Default
    private ScheduleConstraints constraints = new ScheduleConstraints();

    public Duration getDuration() {
        return Duration.between(startTime, endTime);
    }

    public boolean isFlexible() {
        return type == ScheduleType.FLEXIBLE;
    }

    public boolean overlaps(Schedule other) {
        return !this.endTime.isBefore(other.startTime) &&
                !other.endTime.isBefore(this.startTime);
    }

    public boolean canBeMergedWith(Schedule other) {
        // 같은 카테고리이고 30분 이내 거리인 경우 병합 가능
        return this.category == other.category &&
                Math.abs(Duration.between(this.endTime, other.startTime).toMinutes()) <= 30;
    }

    // 정적 팩토리 메소드 - 기본 스케줄 생성
    public static Schedule create(
            LocalDateTime startTime,
            LocalDateTime endTime,
            Location location,
            ScheduleType type,
            int priority,
            String name) {
        return Schedule.builder()
            .startTime(startTime)
            .endTime(endTime)
            .location(location)
            .type(type)
            .priority(priority)
            .name(name)
            .category(PlaceCategory.CULTURE)  // 기본값 설정
            .estimatedDuration(0)
            .expectedCost(0.0)
            .visitPreference(new VisitPreference(false, false, null, 0, 0.0))
            .build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleConstraints {
        private LocalDateTime earliestStartTime;  // 가장 이른 시작 가능 시간
        private LocalDateTime latestEndTime;      // 가장 늦은 종료 가능 시간
        private boolean requiresWeekend;          // 주말 방문 필요 여부
        private int minimumDuration;              // 최소 필요 시간 (분)
        private double maxTravelDistance;         // 최대 이동 거리 (km)
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisitPreference {
        private boolean avoidCrowds;        // 혼잡 회피 선호
        private boolean preferIndoor;        // 실내 선호
        private TransportMode preferredTransportMode; // 선호 이동수단
        private int maxWalkingDistance;     // 최대 도보 거리 (m)
        private double maxBudget;           // 최대 예산
    }
}