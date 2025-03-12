//src/main/java/com/example/TripSpring/service/notification/NavigationNotificationService.java
package com.example.TripSpring.service.notification;

import com.example.TripSpring.dto.navigation.NavigationResponse.NavigationAlert;
import com.example.TripSpring.dto.navigation.NavigationResponse;
import com.example.TripSpring.dto.traffic.TrafficStatus;
import com.example.TripSpring.domain.navigation.RealTimeSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NavigationNotificationService {
    
    public List<NavigationAlert> generateAlerts(
            RealTimeSession session,
            TrafficStatus trafficStatus) {
        List<NavigationAlert> alerts = new ArrayList<>();
        
        // 교통 상황 알림
        if (trafficStatus.getCongestionLevel() > 0.7) {
            alerts.add(NavigationAlert.builder()
                .type(NavigationAlert.AlertType.TRAFFIC)
                .message(String.format("현재 구간 교통 혼잡 (평균 속도: %dkm/h)", 
                    trafficStatus.getAverageSpeed()))
                .severity(NavigationAlert.AlertSeverity.WARNING)
                .build());
        }
        
        // 교통 사고 알림
        for (String incident : trafficStatus.getIncidents()) {
            alerts.add(NavigationAlert.builder()
                .type(NavigationAlert.AlertType.ACCIDENT)
                .message(incident)
                .severity(NavigationAlert.AlertSeverity.WARNING)
                .build());
        }
        
        // 도착 예정 알림
        if (session.getRemainingDistance() < 500) {
            alerts.add(NavigationAlert.builder()
                .type(NavigationAlert.AlertType.ARRIVAL)
                .message(String.format("목적지까지 %dm 남았습니다", 
                    session.getRemainingDistance()))
                .severity(NavigationAlert.AlertSeverity.INFO)
                .build());
        }
        
        // 경로 이탈 알림
        if (session.getStatus() == NavigationResponse.NavigationStatus.REROUTING) {
            alerts.add(NavigationAlert.builder()
                .type(NavigationAlert.AlertType.REROUTE)
                .message("경로를 이탈했습니다. 새로운 경로를 탐색합니다.")
                .severity(NavigationAlert.AlertSeverity.WARNING)
                .build());
        }
        
        // 도착 지연 알림
        if (isDelayed(session)) {
            alerts.add(NavigationAlert.builder()
                .type(NavigationAlert.AlertType.DELAY)
                .message("예상 도착 시간이 지연되었습니다.")
                .severity(NavigationAlert.AlertSeverity.INFO)
                .build());
        }
        
        return alerts;
    }
    
    private boolean isDelayed(RealTimeSession session) {
        // 예상 도착 시간과 실제 예상 시간의 차이가 5분 이상일 때
        if (session.getCurrentRoute() != null) {
            int originalDuration = session.getCurrentRoute().getTotalDuration();
            int currentEstimate = session.getRemainingTime();
            
            return currentEstimate > originalDuration + 300; // 5분 = 300초
        }
        return false;
    }
}