package com.example.TripSpring.dto.response.route;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransitInfo {
    private String lineNumber;        // 노선 번호/호선
    private String transportType;     // 버스/지하철 구분
    private String departureStop;     // 출발 정류장/역
    private String arrivalStop;       // 도착 정류장/역
    private int numberOfStops;        // 정거장 수
    private List<String> transfers;   // 환승 정보
    private int waitingTime;          // 예상 대기 시간
}