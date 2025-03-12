// src/main/java/com/example/TripSpring/util/TransitRouteParser.java
package com.example.TripSpring.util;

import com.example.TripSpring.dto.domain.route.GeoPoint;
import com.example.TripSpring.dto.domain.route.TransportMode;
import com.example.TripSpring.dto.transport.TransitLine;
import com.example.TripSpring.dto.transport.TransitPoint;
import com.example.TripSpring.dto.transport.TransitType;
import com.example.TripSpring.dto.transport.TransportOptionDetails;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TransitRouteParser {
    
    public static List<TransportOptionDetails> parseTransitRoute(String jsonResponse) {
        List<TransportOptionDetails> options = new ArrayList<>();
        
        try {
            JSONObject response = new JSONObject(jsonResponse);
            JSONArray routes = response.getJSONArray("routes");
            
            for (int i = 0; i < routes.length(); i++) {
                JSONObject route = routes.getJSONObject(i);
                options.add(parseRouteOption(route));
            }
            
        } catch (Exception e) {
            log.error("Error parsing transit route: {}", e.getMessage());
        }
        
        return options;
    }
    
    private static TransportOptionDetails parseRouteOption(JSONObject route) {
        List<TransitPoint> transitPoints = new ArrayList<>();
        List<GeoPoint> path = new ArrayList<>();
        
        double totalDistance = 0;
        int totalDuration = 0;
        double totalCost = 0;
        
        JSONArray segments = route.getJSONArray("segments");
        for (int i = 0; i < segments.length(); i++) {
            JSONObject segment = segments.getJSONObject(i);
            
            // 세그먼트별 정보 누적
            totalDistance += segment.getDouble("distance");
            totalDuration += segment.getInt("duration");
            totalCost += segment.getDouble("fare");
            
            // 정류장/역 정보 추가
            if (segment.has("stations")) {
                JSONArray stations = segment.getJSONArray("stations");
                for (int j = 0; j < stations.length(); j++) {
                    transitPoints.add(parseTransitPoint(stations.getJSONObject(j)));
                }
            }
            
            // 경로 좌표 추가
            if (segment.has("path")) {
                JSONArray pathCoords = segment.getJSONArray("path");
                for (int j = 0; j < pathCoords.length(); j++) {
                    JSONObject coord = pathCoords.getJSONObject(j);
                    path.add(new GeoPoint(
                        coord.getDouble("latitude"),
                        coord.getDouble("longitude")
                    ));
                }
            }
        }
        
        // 주요 교통수단 판단
        TransportMode primaryMode = determinePrimaryMode(segments);
        
        return TransportOptionDetails.builder()
            .mode(primaryMode)
            .distance(totalDistance)
            .duration(totalDuration)
            .cost(totalCost)
            .congestion(calculateAverageCongestion(segments))
            .routeDescription(generateRouteDescription(segments))
            .path(path)
            .transitPoints(transitPoints)
            .build();
    }
    
    private static TransitPoint parseTransitPoint(JSONObject station) {
        List<TransitLine> lines = new ArrayList<>();
        
        if (station.has("lines")) {
            JSONArray linesArray = station.getJSONArray("lines");
            for (int i = 0; i < linesArray.length(); i++) {
                JSONObject line = linesArray.getJSONObject(i);
                lines.add(TransitLine.builder()
                    .lineId(line.getString("id"))
                    .lineName(line.getString("name"))
                    .direction(line.getString("direction"))
                    .nextArrivalMinutes(line.optInt("nextArrival", -1))
                    .operator(line.optString("operator"))
                    .fare(line.optDouble("fare", 0.0))
                    .build());
            }
        }
        
        return TransitPoint.builder()
            .location(new GeoPoint(
                station.getDouble("latitude"),
                station.getDouble("longitude")
            ))
            .name(station.getString("name"))
            .type(parseTransitType(station.getString("type")))
            .estimatedTime(LocalDateTime.parse(station.getString("estimatedTime")))
            .availableLines(lines)
            .build();
    }
    
    private static TransitType parseTransitType(String type) {
        return switch (type.toUpperCase()) {
            case "BUS" -> TransitType.BUS_STOP;
            case "SUBWAY" -> TransitType.SUBWAY_STATION;
            case "TRANSFER" -> TransitType.TRANSFER_STATION;
            default -> TransitType.BUS_STOP;
        };
    }
    
    private static TransportMode determinePrimaryMode(JSONArray segments) {
        int busSegments = 0;
        int subwaySegments = 0;
        
        for (int i = 0; i < segments.length(); i++) {
            JSONObject segment = segments.getJSONObject(i);
            String mode = segment.getString("mode").toUpperCase();
            
            if (mode.equals("BUS")) busSegments++;
            else if (mode.equals("SUBWAY")) subwaySegments++;
        }
        
        return subwaySegments >= busSegments ? TransportMode.SUBWAY : TransportMode.BUS;
    }
    
    private static double calculateAverageCongestion(JSONArray segments) {
        double totalCongestion = 0;
        int count = 0;
        
        for (int i = 0; i < segments.length(); i++) {
            JSONObject segment = segments.getJSONObject(i);
            if (segment.has("congestion")) {
                totalCongestion += segment.getDouble("congestion");
                count++;
            }
        }
        
        return count > 0 ? totalCongestion / count : 0.0;
    }
    
    private static String generateRouteDescription(JSONArray segments) {
        StringBuilder description = new StringBuilder();
        
        for (int i = 0; i < segments.length(); i++) {
            JSONObject segment = segments.getJSONObject(i);
            String mode = segment.getString("mode");
            String detail = segment.optString("detail", "");
            
            if (i > 0) description.append(" → ");
            description.append(mode)
                      .append("(")
                      .append(detail)
                      .append(")");
        }
        
        return description.toString();
    }
}