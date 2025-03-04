package com.example.TripSpring.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {
    private String id;
    private String name;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String type; // FIXED 또는 FLEXIBLE
    private int priority; // 우선순위 (1-5)
    private double latitude;
    private double longitude;
    private int duration; // 예상 소요 시간(분)
}
