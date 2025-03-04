//src/main/java/com/example/TripSpring/dto/navigation/TurnByTurnGuide.java
package com.example.TripSpring.dto.navigation;

import com.example.TripSpring.dto.domain.route.GeoPoint;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TurnByTurnGuide {
    private int stepNumber;
    private GeoPoint location;
    private String instruction;
    private TurnType turnType;
    private double distanceToNext;
    private String landmark;
    private String additionalInfo;

    public enum TurnType {
        START("출발"),
        END("도착"),
        STRAIGHT("직진"),
        LEFT("좌회전"),
        RIGHT("우회전"),
        UTURN("유턴"),
        SLIGHT_LEFT("약간 왼쪽"),
        SLIGHT_RIGHT("약간 오른쪽"),
        MERGE("합류"),
        EXIT("출구"),
        TRANSFER("환승"),
        BOARD("승차"),
        ALIGHT("하차");

        private final String description;

        TurnType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}