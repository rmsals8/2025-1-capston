package com.example.TripSpring.service.analysis;

import com.example.TripSpring.dto.request.route.RouteRecommendationRequest.OptimizedSchedule;
import com.example.TripSpring.dto.response.route.FacilityResponse;
import com.example.TripSpring.dto.response.route.RoutePlaceDetailResponse;
import com.example.TripSpring.service.FoursquareService;
import com.example.TripSpring.service.KakaoLocalService;
import com.example.TripSpring.dto.domain.route.CrowdLevel;
import com.example.TripSpring.dto.domain.route.GeoPoint;
import com.example.TripSpring.dto.foursquare.FoursquarePlace;
import com.example.TripSpring.dto.foursquare.FoursquarePlaceDetails;
import com.example.TripSpring.dto.foursquare.FoursquareResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceDetailsAnalyzer {
    private final FoursquareService foursquareService;
    private final KakaoLocalService kakaoLocalService;
    private static final int MAX_RETRY = 3;
    private static final long RETRY_DELAY = 1000L;
    public List<RoutePlaceDetailResponse> analyzeSchedules(List<OptimizedSchedule> schedules) {
        return schedules.stream()
            .map(this::analyzeSinglePlace)
            .collect(Collectors.toList());
    }
    @Cacheable(value = "placeDetails", key = "#placeName")
    private FoursquarePlaceDetails getPlaceDetailsWithRetry(String placeName) {
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                // 1. 장소명으로 Foursquare 검색
                FoursquareResponse searchResponse = foursquareService.searchPlaces(
                    URLEncoder.encode(placeName, StandardCharsets.UTF_8),
                    0.0, // 임시 좌표값
                    0.0,
                    1
                );
    
                // 2. 검색 결과가 있으면 첫 번째 결과의 ID로 상세 정보 조회
                if (searchResponse != null && 
                    searchResponse.getResults() != null && 
                    !searchResponse.getResults().isEmpty()) {
                    
                    FoursquarePlace firstResult = searchResponse.getResults().get(0);
                    // deprecated된 메소드 대신 새로운 메소드 사용
                    return foursquareService.getPlaceDetailsById(firstResult.getFsq_id());
                }
    
                return null;
            } catch (Exception e) {
                log.warn("Attempt {} failed for place {}: {}", 
                    i + 1, placeName, e.getMessage());
                
                if (i < MAX_RETRY - 1) {
                    try {
                        Thread.sleep(RETRY_DELAY * (i + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Operation interrupted", ie);
                    }
                }
            }
        }
        return null;
    }

    private RoutePlaceDetailResponse analyzeSinglePlace(OptimizedSchedule schedule) {
        var response = new RoutePlaceDetailResponse();
        response.setPlaceName(schedule.getName());
        
        try {
            // 1. Foursquare API 호출 - ID 기반 조회로 수정
            FoursquarePlaceDetails foursquareDetails = getPlaceDetailsWithRetry(schedule.getName());
            
            // 2. Kakao Local API 호출
            Map<String, Object> kakaoDetails = null;
            try {
                kakaoDetails = kakaoLocalService.searchPlace(
                    URLEncoder.encode(schedule.getName(), StandardCharsets.UTF_8)
                );
            } catch (Exception e) {
                log.warn("Kakao Local API error for {}: {}", schedule.getName(), e.getMessage());
            }

            // 3. API 응답 데이터 통합
            integrateApiResponses(response, foursquareDetails, kakaoDetails, schedule);

        } catch (Exception e) {
            log.error("Error analyzing place {}: {}", schedule.getName(), e.getMessage());
            setDefaultValues(response);
        }

        return response;
    }
    private void integrateApiResponses(
            RoutePlaceDetailResponse response,
            FoursquarePlaceDetails foursquareDetails,
            Map<String, Object> kakaoDetails,
            OptimizedSchedule schedule) {

        // 1. 혼잡도 설정
        setCrowdLevel(response, foursquareDetails);

        // 2. 최적 방문 시간 설정
        setBestVisitTime(response, schedule.getStartTime().toLocalTime());

        // 3. 주변 시설 정보 설정 (Foursquare API 사용)
        if (foursquareDetails != null && foursquareDetails.getLocation() != null) {
            setNearbyFacilities(response, foursquareDetails);
        }

        // 4. 방문 팁 생성
        generateVisitTips(response, schedule, foursquareDetails, kakaoDetails);
    }

    private void setCrowdLevel(RoutePlaceDetailResponse response, FoursquarePlaceDetails details) {
        if (details != null && details.getRating() != null) {
            double popularity = details.getRating().getRating();
            if (popularity > 9.0) {
                response.setCrowdLevel(CrowdLevel.VERY_HIGH);
            } else if (popularity > 8.0) {
                response.setCrowdLevel(CrowdLevel.HIGH);
            } else if (popularity > 7.0) {
                response.setCrowdLevel(CrowdLevel.MODERATE);
            } else {
                response.setCrowdLevel(CrowdLevel.LOW);
            }
        } else {
            response.setCrowdLevel(CrowdLevel.MODERATE);
        }
    }

    private void setBestVisitTime(RoutePlaceDetailResponse response, LocalTime scheduleTime) {
        // 시간대별 최적 방문 시간 추천
        LocalTime bestTime;
        int hour = scheduleTime.getHour();
        
        if (hour >= 11 && hour <= 13) { // 점심시간
            bestTime = scheduleTime.minusHours(1); // 1시간 일찍 방문 추천
        } else if (hour >= 17 && hour <= 19) { // 저녁시간
            bestTime = scheduleTime.minusHours(1); // 1시간 일찍 방문 추천
        } else {
            bestTime = scheduleTime; // 예정된 시간 유지
        }
        
        response.setBestVisitTime(bestTime);
    }

    private void setNearbyFacilities(RoutePlaceDetailResponse response, 
                                   FoursquarePlaceDetails details) {
        try {
            if (details == null || details.getLocation() == null) {
                response.setNearbyFacilities(new ArrayList<>());
                return;
            }

            List<FacilityResponse> facilities = new ArrayList<>();
            Map<String, String> facilityTypes = getFacilityTypeMappings();

            for (Map.Entry<String, String> facilityType : facilityTypes.entrySet()) {
                try {
                    FoursquareResponse fsqResponse = foursquareService.searchPlaces(
                        facilityType.getValue(),
                        details.getLocation().getLat(),
                        details.getLocation().getLng(),
                        500
                    );

                    if (fsqResponse != null && fsqResponse.getResults() != null) {
                        List<FacilityResponse> typeFacilities = fsqResponse.getResults()
                            .stream()
                            .limit(3)
                            .map(place -> createFacilityResponse(place, facilityType.getKey()))
                            .collect(Collectors.toList());
                        facilities.addAll(typeFacilities);
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch {} facilities: {}", 
                        facilityType.getKey(), e.getMessage());
                }
            }

            facilities.sort(Comparator.comparing(FacilityResponse::getDistance));
            response.setNearbyFacilities(facilities);

        } catch (Exception e) {
            log.error("Error setting nearby facilities: {}", e.getMessage());
            response.setNearbyFacilities(new ArrayList<>());
        }
    }

    private Map<String, String> getFacilityTypeMappings() {
        return Map.of(
            "식당", "restaurant",
            "카페", "cafe",
            "편의점", "convenience",
            "지하철", "subway",
            "버스정류장", "bus_station"
        );
    }

    private FacilityResponse createFacilityResponse(FoursquarePlace place, String type) {
        FacilityResponse facility = new FacilityResponse();
        facility.setName(place.getName());
        facility.setType(type);
        
        if (place.getLocation() != null) {
            facility.setLocation(new GeoPoint(
                place.getLocation().getLat(),
                place.getLocation().getLng()
            ));
        }
        
        facility.setDistance(place.getDistance());
        return facility;
    }

    private void generateVisitTips(
            RoutePlaceDetailResponse response,
            OptimizedSchedule schedule,
            FoursquarePlaceDetails foursquareDetails,
            Map<String, Object> kakaoDetails) {
        
        List<String> tips = new ArrayList<>();
        
        // 1. 시간대별 팁
        LocalTime visitTime = schedule.getStartTime().toLocalTime();
        if (visitTime.getHour() >= 11 && visitTime.getHour() <= 13) {
            tips.add("점심 시간대 방문이므로 혼잡할 수 있습니다. 식사 계획을 미리 세우세요.");
        }
        if (visitTime.getHour() >= 17 && visitTime.getHour() <= 19) {
            tips.add("퇴근 시간대 방문이므로 교통 혼잡을 고려하세요.");
        }

        // 2. 장소별 특성 기반 팁
        if (schedule.getName().contains("궁") || schedule.getName().contains("박물관")) {
            tips.add("관람 시간은 보통 1-2시간 정도 소요됩니다.");
            tips.add("주말에는 해설 프로그램을 이용할 수 있습니다.");
        }
        if (schedule.getName().contains("시장") || schedule.getName().contains("거리")) {
            tips.add("편한 신발을 준비하시고, 군것질을 위한 여유 시간을 가지세요.");
        }

        response.setVisitTips(tips);
    }

    private void setDefaultValues(RoutePlaceDetailResponse response) {
        response.setCrowdLevel(CrowdLevel.MODERATE);
        response.setBestVisitTime(LocalTime.of(10, 0));
        response.setNearbyFacilities(new ArrayList<>());
        response.setVisitTips(Collections.singletonList("방문 전 영업시간을 확인하세요."));
    }
}