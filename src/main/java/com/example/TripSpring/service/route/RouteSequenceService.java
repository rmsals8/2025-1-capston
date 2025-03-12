//src/main/java/com/example/TripSpring/service/route/RouteSequenceService.java
package com.example.TripSpring.service.route;

import com.example.TripSpring.dto.domain.Location;
import com.example.TripSpring.dto.route.OrderedLocation;
import com.example.TripSpring.dto.route.RouteSequence;
import com.example.TripSpring.dto.transport.TransportOptionDetails;
import com.example.TripSpring.service.transport.TransportDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteSequenceService {
    private final TransportDetailsService transportDetailsService;
    private final RouteDetailsService routeDetailsService;

    public RouteSequence createRouteSequence(List<Location> waypoints, LocalDateTime startTime) {
        try {
            // 1. 초기 순서 할당
            List<OrderedLocation> orderedLocations = initializeOrderedLocations(waypoints, startTime);
            
            // 2. 최적 방문 순서 계산
            List<OrderedLocation> optimizedOrder = optimizeVisitationOrder(orderedLocations);
            
            // 3. 구간별 이동 옵션 계산
            Map<Integer, List<TransportOptionDetails>> segmentOptions = 
                calculateSegmentOptions(optimizedOrder);

            // 4. 전체 거리와 시간 계산
            double totalDistance = calculateTotalDistance(segmentOptions);
            int totalDuration = calculateTotalDuration(segmentOptions, optimizedOrder);

            // 5. 최적화 지표 생성
            Map<String, Object> optimizationMetrics = createOptimizationMetrics(
                optimizedOrder, segmentOptions);

            return RouteSequence.builder()
                .sequenceId(UUID.randomUUID().toString())
                .waypoints(optimizedOrder)
                .segmentOptions(segmentOptions)
                .totalDistance(totalDistance)
                .totalDuration(totalDuration)
                .visitationOrder(extractVisitationOrder(optimizedOrder))
                .optimizationMetrics(optimizationMetrics)
                .build();

        } catch (Exception e) {
            log.error("Error creating route sequence: {}", e.getMessage());
            throw new RuntimeException("Failed to create route sequence", e);
        }
    }

    private List<OrderedLocation> initializeOrderedLocations(
            List<Location> waypoints, 
            LocalDateTime startTime) {
        List<OrderedLocation> ordered = new ArrayList<>();
        LocalDateTime currentTime = startTime;

        for (int i = 0; i < waypoints.size(); i++) {
            Location waypoint = waypoints.get(i);
            OrderedLocation orderedLocation = OrderedLocation.builder()
                .sequence(i)
                .location(waypoint)
                .name("Waypoint " + (i + 1))
                .estimatedArrival(currentTime)
                .estimatedDeparture(currentTime.plusMinutes(30))  // 기본 체류시간 30분
                .stayDuration(30)
                .isRequired(i == 0 || i == waypoints.size() - 1)  // 시작과 끝은 필수
                .build();
            
            ordered.add(orderedLocation);
            currentTime = currentTime.plusMinutes(60);  // 이동시간 가정
        }

        return ordered;
    }

    private List<OrderedLocation> optimizeVisitationOrder(List<OrderedLocation> locations) {
        List<OrderedLocation> optimized = new ArrayList<>(locations);
        
        // 시작과 끝점은 고정
        OrderedLocation start = optimized.get(0);
        OrderedLocation end = optimized.get(optimized.size() - 1);
        List<OrderedLocation> middle = optimized.subList(1, optimized.size() - 1);

        // 중간 지점들에 대해 최적화 (여기서는 간단한 Nearest Neighbor 알고리즘 사용)
        List<OrderedLocation> optimizedMiddle = optimizeMiddlePoints(middle, start, end);
        
        // 결과 조합
        List<OrderedLocation> result = new ArrayList<>();
        result.add(start);
        result.addAll(optimizedMiddle);
        result.add(end);

        // 순서 번호 재할당
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setSequence(i);
        }

        return result;
    }

    private List<OrderedLocation> optimizeMiddlePoints(
            List<OrderedLocation> points,
            OrderedLocation start,
            OrderedLocation end) {
        List<OrderedLocation> result = new ArrayList<>();
        List<OrderedLocation> remaining = new ArrayList<>(points);
        OrderedLocation current = start;

        while (!remaining.isEmpty()) {
            OrderedLocation nearest = findNearest(current, remaining);
            if (nearest != null) {
                result.add(nearest);
                remaining.remove(nearest);
                current = nearest;
            }
        }

        return result;
    }

    private OrderedLocation findNearest(OrderedLocation current, List<OrderedLocation> candidates) {
        OrderedLocation nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (OrderedLocation candidate : candidates) {
            double distance = calculateDistance(
                current.getLocation(),
                candidate.getLocation()
            );
            if (distance < minDistance) {
                minDistance = distance;
                nearest = candidate;
            }
        }

        return nearest;
    }

    private double calculateDistance(Location loc1, Location loc2) {
        final int R = 6371; // 지구 반지름 (km)
        double lat1 = Math.toRadians(loc1.getLatitude());
        double lat2 = Math.toRadians(loc2.getLatitude());
        double lon1 = Math.toRadians(loc1.getLongitude());
        double lon2 = Math.toRadians(loc2.getLongitude());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return R * c;
    }

    private Map<Integer, List<TransportOptionDetails>> calculateSegmentOptions(
            List<OrderedLocation> locations) {
        Map<Integer, List<TransportOptionDetails>> options = new HashMap<>();

        for (int i = 0; i < locations.size() - 1; i++) {
            OrderedLocation current = locations.get(i);
            OrderedLocation next = locations.get(i + 1);

            List<TransportOptionDetails> segmentOptions = 
                transportDetailsService.getTransportOptions(
                    current.getLocation(),
                    next.getLocation()
                );

            options.put(i, segmentOptions);
        }

        return options;
    }

    private double calculateTotalDistance(
            Map<Integer, List<TransportOptionDetails>> segmentOptions) {
        return segmentOptions.values().stream()
            .mapToDouble(options -> options.stream()
                .mapToDouble(TransportOptionDetails::getDistance)
                .min()
                .orElse(0.0))
            .sum();
    }

    private int calculateTotalDuration(
            Map<Integer, List<TransportOptionDetails>> segmentOptions,
            List<OrderedLocation> locations) {
        int travelTime = segmentOptions.values().stream()
            .mapToInt(options -> options.stream()
                .mapToInt(TransportOptionDetails::getDuration)
                .min()
                .orElse(0))
            .sum();

        int stayTime = locations.stream()
            .mapToInt(OrderedLocation::getStayDuration)
            .sum();

        return travelTime + stayTime;
    }

    private List<String> extractVisitationOrder(List<OrderedLocation> locations) {
        return locations.stream()
            .map(OrderedLocation::getName)
            .toList();
    }

    private Map<String, Object> createOptimizationMetrics(
            List<OrderedLocation> optimizedOrder,
            Map<Integer, List<TransportOptionDetails>> segmentOptions) {
        Map<String, Object> metrics = new HashMap<>();

        metrics.put("numberOfStops", optimizedOrder.size());
        metrics.put("totalSegments", segmentOptions.size());
        metrics.put("requiredStops", optimizedOrder.stream()
            .filter(OrderedLocation::isRequired)
            .count());

        double averageSegmentDistance = calculateTotalDistance(segmentOptions) / 
            segmentOptions.size();
        metrics.put("averageSegmentDistance", averageSegmentDistance);

        return metrics;
    }
}