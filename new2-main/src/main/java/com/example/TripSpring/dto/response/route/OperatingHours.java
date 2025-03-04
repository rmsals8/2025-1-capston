package com.example.TripSpring.dto.response.route;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperatingHours {
    private boolean isOpen;
    private LocalTime openTime;
    private LocalTime closeTime;
    private List<String> breakTimes;  // 브레이크 타임 정보
    private Map<String, String> weekdayHours;  // 요일별 영업시간
}