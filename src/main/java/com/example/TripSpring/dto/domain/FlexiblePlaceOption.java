package com.example.TripSpring.dto.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlexiblePlaceOption {
    private String id;
    private String name;
    private String type; // 유형 (마트, 서점 등)
    private int priority; // 우선순위 (1-5)
    private int duration; // 예상 소요 시간(분)
}