package com.example.TripSpring.service.calculator;

import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
public class CrowdLevelAnalyzer {
    private static final double RUSH_HOUR_CROWD_LEVEL = 0.8;
    private static final double LUNCH_TIME_CROWD_LEVEL = 0.7;
    private static final double NORMAL_CROWD_LEVEL = 0.4;

    private static final LocalTime MORNING_RUSH_START = LocalTime.of(8, 0);
    private static final LocalTime MORNING_RUSH_END = LocalTime.of(10, 0);
    private static final LocalTime LUNCH_TIME_START = LocalTime.of(11, 30);
    private static final LocalTime LUNCH_TIME_END = LocalTime.of(13, 30);
    private static final LocalTime EVENING_RUSH_START = LocalTime.of(18, 0);
    private static final LocalTime EVENING_RUSH_END = LocalTime.of(20, 0);

    public double analyzeCrowdLevel(LocalDateTime time) {
        LocalTime localTime = time.toLocalTime();
        
        if (isRushHour(localTime)) {
            return RUSH_HOUR_CROWD_LEVEL;
        }
        if (isLunchTime(localTime)) {
            return LUNCH_TIME_CROWD_LEVEL;
        }
        return NORMAL_CROWD_LEVEL;
    }

    public boolean isRushHour(LocalTime localTime) {
            return (localTime.isAfter(MORNING_RUSH_START) && localTime.isBefore(MORNING_RUSH_END)) ||
                   (localTime.isAfter(EVENING_RUSH_START) && localTime.isBefore(EVENING_RUSH_END));
    }

    public boolean isLunchTime(LocalTime time) {
        return time.isAfter(LUNCH_TIME_START) && time.isBefore(LUNCH_TIME_END);
    }
}