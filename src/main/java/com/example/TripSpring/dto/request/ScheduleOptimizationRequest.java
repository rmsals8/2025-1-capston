package com.example.TripSpring.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleOptimizationRequest {
    private List<ScheduleDto> fixedSchedules;
    private List<ScheduleDto> flexibleSchedules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleDto {
        @NotBlank(message = "일정 이름은 필수입니다")
        private String name;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime startTime;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime endTime;

        @NotNull(message = "위치 정보는 필수입니다")
        private LocationDto location;

        @NotBlank(message = "일정 타입은 필수입니다")
        private String type;

        private int priority;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationDto {
        @NotNull(message = "위도는 필수입니다")
        private Double latitude;

        @NotNull(message = "경도는 필수입니다")
        private Double longitude;

        private String address;
        private String placeName;
    }
}