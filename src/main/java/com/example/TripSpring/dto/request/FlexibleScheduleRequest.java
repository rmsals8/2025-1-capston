package com.example.TripSpring.dto.request;

import com.example.TripSpring.domain.Schedule;
import com.example.TripSpring.dto.domain.FlexiblePlaceOption;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class FlexibleScheduleRequest {
    private List<Schedule> fixedSchedules;
    private List<FlexiblePlaceOption> flexibleOptions;
}
