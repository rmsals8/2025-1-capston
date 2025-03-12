package com.example.TripSpring.dto.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NavigationAlert {
    private AlertType type;
    private String message;
    private AlertSeverity severity;

    // Enum을 클래스 외부로 이동
    public static enum AlertType {
        TRAFFIC,
        WEATHER,
        ROUTE_DEVIATION,
        TRANSPORT_INFO,
        SYSTEM
    }

    public static enum AlertSeverity {
        INFO,
        WARNING,
        CRITICAL
    }

    // 커스텀 빌더 클래스 정의
    public static class NavigationAlertBuilder {
        private AlertType type;
        private String message;
        private AlertSeverity severity;

        NavigationAlertBuilder() {
        }

        public NavigationAlertBuilder type(AlertType type) {
            this.type = type;
            return this;
        }

        public NavigationAlertBuilder message(String message) {
            this.message = message;
            return this;
        }

        public NavigationAlertBuilder severity(AlertSeverity severity) {
            this.severity = severity;
            return this;
        }

        public NavigationAlert build() {
            return new NavigationAlert(type, message, severity);
        }
    }

    public static NavigationAlertBuilder builder() {
        return new NavigationAlertBuilder();
    }
}