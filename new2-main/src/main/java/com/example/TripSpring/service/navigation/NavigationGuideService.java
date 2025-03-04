//src/main/java/com/example/TripSpring/service/navigation/NavigationGuideService.java
package com.example.TripSpring.service.navigation;

import com.example.TripSpring.dto.domain.route.GeoPoint;
import com.example.TripSpring.dto.domain.route.TransportMode;
import com.example.TripSpring.dto.navigation.NavigationStep;
import com.example.TripSpring.dto.navigation.TurnByTurnGuide;
import com.example.TripSpring.dto.route.RouteDetails;
import com.example.TripSpring.dto.route.RouteSegmentDetail;
import com.example.TripSpring.dto.route.RouteStep;
import com.example.TripSpring.service.TmapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NavigationGuideService {
    private final TmapService tmapService;

    public List<NavigationStep> generateTurnByTurn(RouteDetails route) {
        List<NavigationStep> steps = new ArrayList<>();
        int totalSteps = countTotalSteps(route);
        int currentStep = 1;
        int remainingDistance = (int) route.getTotalDistance();
        int remainingDuration = route.getTotalDuration();

        // 출발지 추가
        addStartingStep(steps, route, currentStep++, remainingDistance, remainingDuration);

        // 각 세그먼트별 안내 생성
        for (RouteSegmentDetail segment : route.getSegments()) {
            List<NavigationStep> segmentSteps = processSegment(
                segment, 
                currentStep, 
                remainingDistance,
                remainingDuration
            );
            steps.addAll(segmentSteps);
            currentStep += segmentSteps.size();
            remainingDistance -= segment.getDistance();
            remainingDuration -= segment.getDuration();
        }

        // 도착지 추가
        addEndingStep(steps, route, totalSteps);

        return steps;
    }

    private int countTotalSteps(RouteDetails route) {
        return route.getSegments().stream()
            .mapToInt(segment -> segment.getSteps().size())
            .sum() + 2; // 출발지와 도착지 포함
    }

private void addStartingStep(
    List<NavigationStep> steps,
    RouteDetails route,
    int stepNumber,
    int remainingDistance,
    int remainingDuration) {
    
    RouteSegmentDetail firstSegment = route.getSegments().get(0);
    
    // GeoPoint 객체 생성
    GeoPoint startLocation = new GeoPoint(
        firstSegment.getStartLocation().getLatitude(),
        firstSegment.getStartLocation().getLongitude()
    );

    steps.add(NavigationStep.builder()
        .guide(TurnByTurnGuide.builder()
            .stepNumber(stepNumber)
            .location(startLocation)  // GeoPoint 객체 전달
            .instruction("출발지에서 " + firstSegment.getInstruction())
            .turnType(TurnByTurnGuide.TurnType.START)
            .distanceToNext(firstSegment.getDistance())
            .build())
        .estimatedTime(LocalDateTime.now())
        .transportMode(firstSegment.getMode())
        .trafficCondition(getTrafficCondition(firstSegment.getCongestion()))
        .isTransferPoint(false)
        .remainingDistance(remainingDistance)
        .remainingDuration(remainingDuration)
        .build());
}
    private List<NavigationStep> processSegment(
            RouteSegmentDetail segment,
            int startingStep,
            int remainingDistance,
            int remainingDuration) {
        
        List<NavigationStep> steps = new ArrayList<>();
        LocalDateTime currentTime = LocalDateTime.now();
        int stepNumber = startingStep;

        for (RouteStep step : segment.getSteps()) {
            TurnByTurnGuide guide = TurnByTurnGuide.builder()
                .stepNumber(stepNumber++)
                .location(step.getLocation())
                .instruction(generateInstruction(step, segment.getMode()))
                .turnType(convertTurnType(step.getType()))
                .distanceToNext(step.getDistanceToNext())
                .landmark(step.getAdditionalInfo())
                .build();

            NavigationStep navStep = NavigationStep.builder()
                .guide(guide)
                .estimatedTime(currentTime.plusSeconds((long)(step.getDistanceToNext() / getAverageSpeed(segment.getMode()))))
                .transportMode(segment.getMode())
                .trafficCondition(getTrafficCondition(segment.getCongestion()))
                .isTransferPoint(isTransferPoint(step))
                .remainingDistance(remainingDistance)
                .remainingDuration(remainingDuration)
                .build();

            steps.add(navStep);
            
            // Update remaining values
            remainingDistance -= step.getDistanceToNext();
            remainingDuration -= (int)(step.getDistanceToNext() / getAverageSpeed(segment.getMode()));
        }

        return steps;
    }

// NavigationGuideService.java의 addEndingStep 메소드 수정
private void addEndingStep(List<NavigationStep> steps, RouteDetails route, int stepNumber) {
    RouteSegmentDetail lastSegment = route.getSegments().get(route.getSegments().size() - 1);
    
    // GeoPoint 객체 생성
    GeoPoint endLocation = new GeoPoint(
        lastSegment.getEndLocation().getLatitude(),
        lastSegment.getEndLocation().getLongitude()
    );
    
    steps.add(NavigationStep.builder()
        .guide(TurnByTurnGuide.builder()
            .stepNumber(stepNumber)
            .location(endLocation)  // GeoPoint 객체 전달
            .instruction("목적지에 도착했습니다")
            .turnType(TurnByTurnGuide.TurnType.END)
            .distanceToNext(0)
            .build())
        .estimatedTime(LocalDateTime.now().plusMinutes(route.getTotalDuration()))
        .transportMode(lastSegment.getMode())
        .trafficCondition("정상")
        .isTransferPoint(false)
        .remainingDistance(0)
        .remainingDuration(0)
        .build());
}
    private String generateInstruction(RouteStep step, TransportMode mode) {
        StringBuilder instruction = new StringBuilder();
        
        if (mode == TransportMode.WALK) {
            instruction.append("도보로 ");
        } else if (mode == TransportMode.BUS || mode == TransportMode.SUBWAY) {
            instruction.append(mode == TransportMode.BUS ? "버스로 " : "지하철로 ");
        }

        instruction.append(step.getInstruction());
        
        if (step.getDistanceToNext() > 0) {
            instruction.append(String.format(" %.0fm", step.getDistanceToNext()));
        }

        return instruction.toString();
    }

    private TurnByTurnGuide.TurnType convertTurnType(RouteStep.StepType stepType) {
        return switch (stepType) {
            case START -> TurnByTurnGuide.TurnType.START;
            case END -> TurnByTurnGuide.TurnType.END;
            case STRAIGHT -> TurnByTurnGuide.TurnType.STRAIGHT;
            case LEFT -> TurnByTurnGuide.TurnType.LEFT;
            case RIGHT -> TurnByTurnGuide.TurnType.RIGHT;
            case UTURN -> TurnByTurnGuide.TurnType.UTURN;
            case SLIGHT_LEFT -> TurnByTurnGuide.TurnType.SLIGHT_LEFT;
            case SLIGHT_RIGHT -> TurnByTurnGuide.TurnType.SLIGHT_RIGHT;
            case MERGE -> TurnByTurnGuide.TurnType.MERGE;
            case EXIT -> TurnByTurnGuide.TurnType.EXIT;
            case TRANSFER -> TurnByTurnGuide.TurnType.TRANSFER;
            case BOARD -> TurnByTurnGuide.TurnType.BOARD;
            case ALIGHT -> TurnByTurnGuide.TurnType.ALIGHT;
        };
    }

    private String getTrafficCondition(double congestion) {
        if (congestion < 0.3) return "원활";
        if (congestion < 0.7) return "보통";
        return "혼잡";
    }

    private double getAverageSpeed(TransportMode mode) {
        return switch (mode) {
            case WALK -> 4.0;     // 4km/h
            case BUS -> 20.0;     // 20km/h
            case SUBWAY -> 40.0;  // 40km/h
            case TAXI -> 30.0;    // 30km/h
        };
    }

    private boolean isTransferPoint(RouteStep step) {
        return step.getType() == RouteStep.StepType.TRANSFER ||
               step.getType() == RouteStep.StepType.BOARD ||
               step.getType() == RouteStep.StepType.ALIGHT;
    }
}