package com.example.TripSpring.dto.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.example.TripSpring.dto.domain.route.TransportMode;
import com.example.TripSpring.dto.request.LocationUpdate;
import com.example.TripSpring.dto.request.StartNavigationRequest;
import com.example.TripSpring.dto.response.NavigationStatus;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class NavigationSession {
    private String navigationId;
    private List<RouteSegment> segments;
    private int currentSegmentIndex;
    private Location currentLocation;
    private NavigationStatus.Status status;
    private Double progress;
    private LocalDateTime lastUpdated;
    private Location destination;
    private double totalDistance;
    private int totalTime;
    private int totalFare;
    private Route currentRoute;
    private List<String> upcomingInstructions;
    private LocalDateTime lastUpdate;

    public NavigationSession(String navigationId, 
                           List<StartNavigationRequest.RouteSelection> selectedRoutes, 
                           StartNavigationRequest.Location initialLocation) {
        this.navigationId = navigationId;
        this.currentLocation = convertToLocation(initialLocation);
        this.status = NavigationStatus.Status.STARTED;
        this.progress = 0.0;
        this.currentSegmentIndex = 0;
        this.lastUpdated = LocalDateTime.now();
        this.upcomingInstructions = new ArrayList<>();
        
        this.segments = selectedRoutes.stream()
            .map(this::createSegment)
            .collect(Collectors.toList());
    }
    
    public NavigationSession(String navigationId, Location currentLocation, Location destination) {
        this.navigationId = navigationId;
        this.currentLocation = currentLocation;
        this.destination = destination;
        this.upcomingInstructions = new ArrayList<>();
        this.lastUpdate = LocalDateTime.now();
        this.segments = new ArrayList<>();
        this.status = NavigationStatus.Status.STARTED;
        this.progress = 0.0;
        this.currentSegmentIndex = 0;
    }

    public NavigationSession() {
        this.navigationId = UUID.randomUUID().toString();
        this.segments = new ArrayList<>();
        this.currentSegmentIndex = 0;
        this.progress = 0.0;
        this.status = NavigationStatus.Status.STARTED;
        this.lastUpdated = LocalDateTime.now();
        this.upcomingInstructions = new ArrayList<>();
    }

    public void updateCurrentLocation(Location location) {
        this.currentLocation = location;
        this.lastUpdated = LocalDateTime.now();
    }

    public void updateCurrentLocation(LocationUpdate update) {
        this.currentLocation = new Location(update.getLatitude(), update.getLongitude());
        this.lastUpdated = update.getTimestamp() != null ? 
            update.getTimestamp() : LocalDateTime.now();
    }

    public Location getDestination() {
        if (segments != null && !segments.isEmpty()) {
            return segments.get(segments.size() - 1).getEndLocation();
        }
        return destination;
    }

    public void setDestination(Location destination) {
        this.destination = destination;
    }

    public Route getCurrentRoute() {
        return this.currentRoute;
    }

    public void updateTotalInfo(int totalDistance, int totalTime, int totalFare) {
        this.totalDistance = totalDistance;
        this.totalTime = totalTime;
        this.totalFare = totalFare;
        this.lastUpdated = LocalDateTime.now();
        log.debug("Updated total info - distance: {}, time: {}, fare: {}", 
            totalDistance, totalTime, totalFare);
    }

    public RouteSegment getCurrentSegment() {
        if (segments == null || segments.isEmpty() || currentSegmentIndex >= segments.size()) {
            return null;
        }
        return segments.get(currentSegmentIndex);
    }

    public boolean hasNextSegment() {
        return segments != null && currentSegmentIndex < segments.size() - 1;
    }

    public void moveToNextSegment() {
        if (hasNextSegment()) {
            currentSegmentIndex++;
            progress = 0.0;
        }
    }

    public void updateProgress(double newProgress) {
        this.progress = newProgress;
        this.lastUpdated = LocalDateTime.now();
    }

    public void complete() {
        this.status = NavigationStatus.Status.COMPLETED;
        this.progress = 100.0;
        this.lastUpdated = LocalDateTime.now();
    }

    public List<Location> getRemainingDestinations() {
        if (segments == null) {
            return new ArrayList<>();
        }
        return segments.stream()
            .skip(currentSegmentIndex)
            .map(RouteSegment::getEndLocation)
            .collect(Collectors.toList());
    }

    private RouteSegment createSegment(StartNavigationRequest.RouteSelection selection) {
        RouteSegment segment = new RouteSegment();
        segment.setSegmentId(selection.getSegmentId());
        segment.setStartLocation(convertToLocation(selection.getStartLocation()));
        segment.setEndLocation(convertToLocation(selection.getEndLocation()));
        segment.setTransportMode(selection.getTransportMode());
        segment.setDistance(selection.getDistance());
        segment.setEstimatedDuration(selection.getEstimatedDuration());
        return segment;
    }

    public void updateRoute(Route newRoute) {
        if (newRoute == null) {
            log.error("Invalid route update received");
            return;
        }

        this.currentRoute = newRoute;

        if (newRoute.getSchedules() != null) {
            this.segments = new ArrayList<>();
            for (Schedule schedule : newRoute.getSchedules()) {
                RouteSegment segment = new RouteSegment();
                segment.setSegmentId(UUID.randomUUID().toString());
                segment.setStartLocation(schedule.getLocation());
                
                if (newRoute.getSchedules().indexOf(schedule) < newRoute.getSchedules().size() - 1) {
                    segment.setEndLocation(newRoute.getSchedules().get(
                        newRoute.getSchedules().indexOf(schedule) + 1).getLocation());
                } else if (destination != null) {
                    segment.setEndLocation(destination);
                }
                
                segment.setDistance(newRoute.getTotalDistance() / newRoute.getSchedules().size());
                segment.setEstimatedDuration((int) (newRoute.getTotalTime() / newRoute.getSchedules().size()));
                segments.add(segment);
            }
        }
        
        updateTotalInfo(
            (int) newRoute.getTotalDistance(),
            (int) newRoute.getTotalTime(),
            (int) newRoute.getTotalCost()
        );
        
        this.currentSegmentIndex = 0;
        this.progress = 0.0;
        this.lastUpdated = LocalDateTime.now();
    }

    private TransportMode convertTransportMode(String mode) {
        return TransportMode.valueOf(mode);
    }

    private Location convertToLocation(StartNavigationRequest.Location reqLocation) {
        return new Location(
            reqLocation.getLatitude(),
            reqLocation.getLongitude(),
            reqLocation.getName()
        );
    }

    private String generateInitialInstruction(TransportMode mode) {
        return switch (mode) {
            case WALK -> "도보 경로를 안내합니다";
            case BUS -> "버스 경로를 안내합니다";
            case SUBWAY -> "지하철 경로를 안내합니다";
            case TAXI -> "택시 경로를 안내합니다";
            default -> "경로 안내를 시작합니다";
        };
    }
}