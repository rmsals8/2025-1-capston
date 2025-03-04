package com.example.TripSpring.service.scheduler;


import com.example.TripSpring.dto.Location;
import com.example.TripSpring.dto.analysis.ScheduleAnalysis;
import com.example.TripSpring.dto.analysis.ScheduleAnalysis.ScheduleDecision;
import com.example.TripSpring.dto.domain.PlaceInfo;
import com.example.TripSpring.dto.domain.Schedule;
import com.example.TripSpring.dto.domain.TrafficInfo;
import com.example.TripSpring.service.FirstMapService;
import com.example.TripSpring.service.LocationInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleCSPSolver {
    private final LocationInfoService locationInfoService;
    private final FirstMapService firstMapService;
    private Map<String, PlaceInfo> placeInfoCache = new HashMap<>();
    private Map<String, TrafficInfo> trafficInfoCache = new HashMap<>();
    // 시간 슬롯 간격 (분)
    private static final int TIME_SLOT_INTERVAL = 15;
    // 최소 방문 시간 (분)
    private static final int MIN_VISIT_DURATION = 30;
    // 이동 시간 버퍼 (분)
    private static final int TRAVEL_TIME_BUFFER = 10;
    private static final int MIN_SLOT_DURATION = 30; // 최소 슬롯 시간
    private static final LocalTime BUSINESS_START = LocalTime.of(9, 0);
    private static final LocalTime BUSINESS_END = LocalTime.of(21, 0);
    public List<Schedule> solveSchedule(List<Schedule> fixedSchedules, List<Schedule> flexibleSchedules) {
        List<Schedule> allSchedules = new ArrayList<>(fixedSchedules);
        List<TimeSlot> availableSlots = generateTimeSlots(fixedSchedules);
        
        // 유연한 일정 처리 로직 개선
        for (Schedule flexible : flexibleSchedules) {
            Optional<TimeSlot> bestSlot = findBestTimeSlot(flexible, availableSlots, allSchedules);
            
            if (bestSlot.isPresent()) {
                TimeSlot slot = bestSlot.get();
                flexible.setStartTime(slot.getStart());
                flexible.setEndTime(slot.getStart().plusMinutes(MIN_SLOT_DURATION));
                allSchedules.add(flexible);
                
                // 사용된 슬롯과 겹치는 슬롯 제거
                updateAvailableSlots(availableSlots, slot, flexible, allSchedules);
    
                
                log.info("Scheduled {} from {} to {}", 
                    flexible.getName(), 
                    flexible.getStartTime().format(DateTimeFormatter.ISO_LOCAL_TIME),
                    flexible.getEndTime().format(DateTimeFormatter.ISO_LOCAL_TIME));
            } else {
                log.warn("Could not find suitable time slot for: {}", flexible.getName());
                // 실패 원인 기록
                recordSchedulingFailure(flexible, availableSlots);
            }
        }
        
        return allSchedules;
    }
    private void recordSchedulingFailure(Schedule schedule, List<TimeSlot> availableSlots) {
        StringBuilder reason = new StringBuilder();
        reason.append("Failed to schedule ").append(schedule.getName()).append(":\n");
        
        if (availableSlots.isEmpty()) {
            reason.append("- No available time slots\n");
        } else {
            reason.append("- Available slots: ").append(availableSlots.size()).append("\n");
            reason.append("- First available: ")
                .append(availableSlots.get(0).getStart().format(DateTimeFormatter.ISO_LOCAL_TIME))
                .append("\n");
            reason.append("- Last available: ")
                .append(availableSlots.get(availableSlots.size()-1).getStart().format(DateTimeFormatter.ISO_LOCAL_TIME))
                .append("\n");
        }
        
        log.info(reason.toString());
    }

    private List<TimeSlot> generateTimeSlots(List<Schedule> fixedSchedules) {
        List<TimeSlot> slots = new ArrayList<>();
        LocalDateTime current = fixedSchedules.get(0).getStartTime();
        LocalDateTime endTime = fixedSchedules.get(fixedSchedules.size() - 1).getEndTime();
        
        // 더 세밀한 시간 슬롯 생성
        while (current.plusMinutes(MIN_SLOT_DURATION).isBefore(endTime)) {
            TimeSlot slot = new TimeSlot(
                current,
                current.plusMinutes(MIN_SLOT_DURATION),
                calculateSlotPreference(current)
            );
            
            // 고정 일정과 겹치지 않는 경우만 추가
            if (isSlotAvailable(slot, fixedSchedules)) {
                slots.add(slot);
            }
            
            current = current.plusMinutes(15); // 15분 단위로 슬롯 생성
        }
        
        return slots;
    }
    private boolean isSlotAvailable(TimeSlot slot, List<Schedule> fixedSchedules) {
        return fixedSchedules.stream().noneMatch(fixed -> 
            slot.getStart().isBefore(fixed.getEndTime()) &&
            slot.getEnd().isAfter(fixed.getStartTime())
        );
    }
    
    private double calculateSlotPreference(LocalDateTime time) {
        LocalTime timeOfDay = time.toLocalTime();
        
        // 점심 시간대 선호도 높임
        if (timeOfDay.isAfter(LocalTime.of(11, 30)) && 
            timeOfDay.isBefore(LocalTime.of(13, 30))) {
            return 0.3; // 가장 선호
        }
        
        // 오후 시간대 선호도
        if (timeOfDay.isAfter(LocalTime.of(13, 30)) && 
            timeOfDay.isBefore(LocalTime.of(15, 0))) {
            return 0.5;
        }
        
        return 1.0; // 기본 선호도
    }

    private double calculateSlotCost(LocalDateTime time) {
        LocalTime timeOfDay = time.toLocalTime();
        // 점심시간(11:30-13:30)에 가중치 부여
        if (timeOfDay.isAfter(LocalTime.of(11, 30)) && 
            timeOfDay.isBefore(LocalTime.of(13, 30))) {
            return 0.5; // 낮은 비용 = 높은 선호도
        }
        return 1.0;
    }

    private Optional<TimeSlot> findBestTimeSlot(
            Schedule flexible,
            List<TimeSlot> availableSlots,
            List<Schedule> existingSchedules) {
        
        // 장소 정보를 미리 한 번만 가져오기

        
        log.debug("Finding best time slot for {} among {} available slots", 
            flexible.getName(), availableSlots.size());
        
        return availableSlots.stream()
            .filter(slot -> isSlotValid(slot, flexible, existingSchedules))
            .min(Comparator.comparing(TimeSlot::getCost));
    }
    private List<TimeSlot> filterValidSlots(List<TimeSlot> slots, List<Schedule> fixedSchedules) {
        return slots.stream()
            .filter(slot -> {
                // 고정 일정과 겹치지 않는지 확인
                return fixedSchedules.stream().noneMatch(fixed ->
                    slot.getStart().isBefore(fixed.getEndTime()) &&
                    slot.getEnd().isAfter(fixed.getStartTime())
                );
            })
            .collect(Collectors.toList());
    }

    private boolean isWithinOperatingHours(TimeSlot slot, PlaceInfo placeInfo) {
        LocalTime slotStartTime = slot.getStart().toLocalTime();
        LocalTime slotEndTime = slot.getEnd().toLocalTime();
        
        return slotStartTime.isAfter(placeInfo.getOpenTime()) &&
            slotEndTime.isBefore(placeInfo.getCloseTime());
    }

    private boolean hasEnoughTravelTime(TimeSlot slot, Schedule flexible, List<Schedule> existingSchedules) {
        Schedule previous = findPreviousSchedule(slot.getStart(), existingSchedules);
        if (previous != null) {
            TrafficInfo trafficInfo = firstMapService.getTrafficInfo(
                previous.getLocation(),
                flexible.getLocation()
            );
            // 이전 일정 종료 후 이동시간 + 버퍼시간(15분) 확인
            if (previous.getEndTime()
                .plusMinutes(trafficInfo.getEstimatedTime())
                .plusMinutes(15)
                .isAfter(slot.getStart())) {
                return false;
            }
        }

        Schedule next = findNextSchedule(slot.getEnd(), existingSchedules);
        if (next != null) {
            TrafficInfo trafficInfo = firstMapService.getTrafficInfo(
                flexible.getLocation(),
                next.getLocation()
            );
            // 다음 일정 시작 전 이동시간 + 버퍼시간(15분) 확인
            if (slot.getEnd()
                .plusMinutes(trafficInfo.getEstimatedTime())
                .plusMinutes(15)
                .isAfter(next.getStartTime())) {
                return false;
            }
        }

        return true;
    }

    private boolean isSlotValid(TimeSlot slot, Schedule flexible, List<Schedule> existingSchedules) {
        // 1. 영업시간 체크 - 여유 시간 추가
        PlaceInfo placeInfo = locationInfoService.getPlaceInfo(flexible.getName());
        if (placeInfo != null) {
            LocalTime slotStart = slot.getStart().toLocalTime();
            LocalTime slotEnd = slot.getEnd().toLocalTime();
            
            if (slotStart.isBefore(placeInfo.getOpenTime().plusMinutes(15)) ||
                slotEnd.isAfter(placeInfo.getCloseTime().minusMinutes(15))) {
                return false;
            }
        }
        
        // 2. 이동시간 체크 - 버퍼 시간 조정
        Schedule previous = findPreviousSchedule(slot.getStart(), existingSchedules);
        if (previous != null) {
            try {
                TrafficInfo trafficInfo = firstMapService.getTrafficInfo(
                    previous.getLocation(),
                    flexible.getLocation()
                );
    
                // 3시간 이상의 이동시간은 보정
                if (trafficInfo.getEstimatedTime() > 180) {
                    trafficInfo = new TrafficInfo(
                        trafficInfo.getTrafficRate(),
                        (int)(trafficInfo.getDistance() * 3),
                        trafficInfo.getDistance()
                    );
                }
                
                if (previous.getEndTime()
                    .plusMinutes(trafficInfo.getEstimatedTime())
                    .plusMinutes(TRAVEL_TIME_BUFFER)
                    .isAfter(slot.getStart())) {
                    return false;
                }
            } catch (Exception e) {
                log.warn("Failed to get traffic info for previous schedule: {}", e.getMessage());
                // API 호출 실패 시 기본값 사용
                if (previous.getEndTime()
                    .plusMinutes(30)
                    .plusMinutes(TRAVEL_TIME_BUFFER)
                    .isAfter(slot.getStart())) {
                    return false;
                }
            }
        }
        
        Schedule next = findNextSchedule(slot.getEnd(), existingSchedules);
        if (next != null) {
            try {
                TrafficInfo trafficInfo = firstMapService.getTrafficInfo(
                    flexible.getLocation(),
                    next.getLocation()
                );
    
                // 3시간 이상의 이동시간은 보정
                if (trafficInfo.getEstimatedTime() > 180) {
                    trafficInfo = new TrafficInfo(
                        trafficInfo.getTrafficRate(),
                        (int)(trafficInfo.getDistance() * 3),
                        trafficInfo.getDistance()
                    );
                }
                
                if (slot.getEnd()
                    .plusMinutes(trafficInfo.getEstimatedTime())
                    .plusMinutes(TRAVEL_TIME_BUFFER)
                    .isAfter(next.getStartTime())) {
                    return false;
                }
            } catch (Exception e) {
                log.warn("Failed to get traffic info for next schedule: {}", e.getMessage());
                // API 호출 실패 시 기본값 사용
                if (slot.getEnd()
                    .plusMinutes(30)
                    .plusMinutes(TRAVEL_TIME_BUFFER)
                    .isAfter(next.getStartTime())) {
                    return false;
                }
            }
        }
        
        return true;
    }
    private void updateAvailableSlots(List<TimeSlot> slots, TimeSlot usedSlot, Schedule schedule, List<Schedule> allSchedules) {
        // 1. 직접적으로 사용된 시간 슬롯과 겹치는 슬롯들 제거
        slots.removeIf(slot -> 
            slot.getStart().isEqual(usedSlot.getStart()) ||
            (slot.getStart().isAfter(usedSlot.getStart()) && 
             slot.getEnd().isBefore(usedSlot.getEnd().plusMinutes(MIN_SLOT_DURATION)))
        );
    
        // 2. 이동 시간을 고려한 버퍼 시간 계산
        if (!slots.isEmpty() && schedule.getLocation() != null) {
            // 이전 일정들과의 이동 시간 고려
            slots.removeIf(slot -> {
                Schedule prevSchedule = findPreviousSchedule(slot.getStart(), allSchedules);
                if (prevSchedule != null) {
                    TrafficInfo traffic = firstMapService.getTrafficInfo(
                        prevSchedule.getLocation(),
                        schedule.getLocation()
                    );
                    // 이전 일정 종료 후 이동시간 + 버퍼시간을 고려했을 때 슬롯 시작 시간에 도달할 수 없는 경우
                    return prevSchedule.getEndTime()
                        .plusMinutes(traffic.getEstimatedTime())
                        .plusMinutes(TRAVEL_TIME_BUFFER)
                        .isAfter(slot.getStart());
                }
                return false;
            });
    
            // 다음 일정들과의 이동 시간 고려
            slots.removeIf(slot -> {
                Schedule nextSchedule = findNextSchedule(slot.getEnd(), allSchedules);
                if (nextSchedule != null) {
                    TrafficInfo traffic = firstMapService.getTrafficInfo(
                        schedule.getLocation(),
                        nextSchedule.getLocation()
                    );
                    // 슬롯 종료 후 이동시간 + 버퍼시간을 고려했을 때 다음 일정에 도달할 수 없는 경우
                    return slot.getEnd()
                        .plusMinutes(traffic.getEstimatedTime())
                        .plusMinutes(TRAVEL_TIME_BUFFER)
                        .isAfter(nextSchedule.getStartTime());
                }
                return false;
            });
        }
    
        log.debug("Updated available slots. Remaining slots: {}", slots.size());
    }

    private double calculateCost(LocalDateTime time) {
        // 점심/저녁 시간대 선호도 반영
        LocalTime timeOfDay = time.toLocalTime();
        if ((timeOfDay.isAfter(LocalTime.of(11, 30)) && 
             timeOfDay.isBefore(LocalTime.of(13, 30))) ||
            (timeOfDay.isAfter(LocalTime.of(18, 0)) && 
             timeOfDay.isBefore(LocalTime.of(20, 0)))) {
            return 0.5; // 선호 시간대
        }
        return 1.0;
    }

    private Schedule findPreviousSchedule(LocalDateTime time, List<Schedule> schedules) {
        return schedules.stream()
            .filter(s -> s.getEndTime().isBefore(time))
            .max(Comparator.comparing(Schedule::getEndTime))
            .orElse(null);
    }

    private Schedule findNextSchedule(LocalDateTime time, List<Schedule> schedules) {
        return schedules.stream()
            .filter(s -> s.getStartTime().isAfter(time))
            .min(Comparator.comparing(Schedule::getStartTime))
            .orElse(null);
    }
    private double calculateTravelTime(Schedule from, Schedule to) {
        if (from == null || to == null) return 0.0;
        TrafficInfo info = firstMapService.getTrafficInfo(from.getLocation(), to.getLocation());
        return info.getEstimatedTime();
    }

    private double calculatePopularityScore(LocalDateTime time) {
        LocalTime timeOfDay = time.toLocalTime();
        if (timeOfDay.isAfter(LocalTime.of(11, 30)) && 
            timeOfDay.isBefore(LocalTime.of(13, 30))) {
            return 0.8; // 점심 시간대
        } else if (timeOfDay.isAfter(LocalTime.of(17, 0)) && 
                timeOfDay.isBefore(LocalTime.of(19, 0))) {
            return 0.7; // 저녁 시간대
        }
        return 0.5;
    }

}

@lombok.Value
class TimeSlot {
    LocalDateTime start;
    LocalDateTime end;
    double cost;
}