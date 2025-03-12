package com.example.TripSpring.service;

import com.example.TripSpring.dto.Geometry;
import com.example.TripSpring.dto.Place;
import com.example.TripSpring.dto.domain.Location;
import com.example.TripSpring.dto.domain.PlaceInfo;
import com.example.TripSpring.dto.domain.Schedule;
import com.example.TripSpring.dto.domain.TrafficInfo;
import com.example.TripSpring.dto.foursquare.FoursquarePlace;
import com.example.TripSpring.dto.foursquare.FoursquarePlaceDetails;
import com.example.TripSpring.dto.foursquare.FoursquareResponse;
import com.example.TripSpring.service.calculator.CrowdLevelAnalyzer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.HttpHeaders;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class APIIntegrationService {
    private final FirstMapService firstMapService;
    private final FoursquareService foursquareService;
    private final LocationInfoService locationInfoService;
    private final CrowdLevelAnalyzer crowdLevelAnalyzer;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    // API 키 설정
    @Value("${app.api.kakao}")
    private String kakaoApiKey;
    @Value("${app.api.google}")
    private String googleApiKey;
    @Value("${app.api.naver.client-id}")
    private String naverClientId;
    @Value("${app.api.naver.client-secret}")
    private String naverClientSecret;
    @Value("${app.api.foursquare}")
    private String foursquareApiKey;

    // 마트 관련 카테고리 ID 리스트
    private static final List<String> MART_CATEGORIES = Arrays.asList(
        "MT1", // 대형마트 (카카오 카테고리)
        "CS2", // 편의점 (카카오 카테고리)
        "17000", // 상점 및 서비스 (Foursquare 카테고리)
        "17069", // 슈퍼마켓 (Foursquare 카테고리)
        "grocery", // Google 카테고리
        "supermarket", // Google 카테고리
        "convenience_store" // Google 카테고리
    );

    /**
     * 장소 검색 메인 메서드 - 경로 기반으로 검색
     * 두 고정 일정 사이의 경로를 따라 장소 검색
     */
    public List<Place> searchPlacesBetweenSchedules(
            String placeType, Schedule prevSchedule, Schedule nextSchedule, int maxResults) {
        try {
            log.info("Searching for '{}' between {} and {}", 
                    placeType, prevSchedule.getName(), nextSchedule.getName());
            
            // 출발지와 도착지 좌표
            double startLat = prevSchedule.getLocation().getLatitude();
            double startLon = prevSchedule.getLocation().getLongitude();
            double endLat = nextSchedule.getLocation().getLatitude();
            double endLon = nextSchedule.getLocation().getLongitude();
            
            // 경로 상의 여러 지점 샘플링 (5개 지점)
            List<Location> routePoints = samplePointsAlongRoute(
                    startLat, startLon, endLat, endLon, 5);
            
            log.info("Generated {} sample points along route", routePoints.size());
            
            // 여러 검색어 목록 준비
            List<String> searchTerms = getSearchTerms(placeType);
            log.info("Search terms: {}", searchTerms);
            
            // 결과 저장용 컬렉션
            List<Place> allResults = new ArrayList<>();
            
            // 각 경로 지점에서 검색 수행
            for (Location point : routePoints) {
                // 각 검색어로 시도
                for (String term : searchTerms) {
                    // 최대 반경 (15km)
                    int searchRadius = 15000;
                    
                    // 1. Google API 시도
                    List<Place> googleResults = searchNearbyPlacesWithGoogle(
                            term, point.getLatitude(), point.getLongitude(), searchRadius);
                    if (!googleResults.isEmpty()) {
                        log.info("Found {} places using Google API at point ({}, {})", 
                                googleResults.size(), point.getLatitude(), point.getLongitude());
                        allResults.addAll(googleResults);
                    }
                    
                    // 2. Kakao API 시도
                    List<Place> kakaoResults = searchNearbyPlacesWithKakao(
                            term, point.getLatitude(), point.getLongitude(), searchRadius);
                    if (!kakaoResults.isEmpty()) {
                        log.info("Found {} places using Kakao API at point ({}, {})", 
                                kakaoResults.size(), point.getLatitude(), point.getLongitude());
                        allResults.addAll(kakaoResults);
                    }
                    
                    // 3. Naver API 시도
                    List<Place> naverResults = searchNearbyPlacesWithNaver(
                            term, point.getLatitude(), point.getLongitude(), searchRadius);
                    if (!naverResults.isEmpty()) {
                        log.info("Found {} places using Naver API at point ({}, {})", 
                                naverResults.size(), point.getLatitude(), point.getLongitude());
                        allResults.addAll(naverResults);
                    }
                    
                    // 4. Foursquare API 시도
                    List<Place> foursquareResults = searchNearbyPlacesWithFoursquare(
                            term, point.getLatitude(), point.getLongitude(), searchRadius);
                    if (!foursquareResults.isEmpty()) {
                        log.info("Found {} places using Foursquare API at point ({}, {})", 
                                foursquareResults.size(), point.getLatitude(), point.getLongitude());
                        allResults.addAll(foursquareResults);
                    }
                }
            }
            
            // 중복 제거 (같은 장소 ID의 경우 하나만 유지)
            List<Place> uniqueResults = removeDuplicates(allResults);
            log.info("Found {} unique places after removing duplicates", uniqueResults.size());
            
            // 관련성 필터링
            List<Place> filteredResults = filterPlacesByRelevance(uniqueResults, placeType);
            log.info("Filtered to {} relevant places", filteredResults.size());
            
            // 경로와의 거리 기준으로 정렬
            List<Place> rankedResults = rankPlacesByRouteProximity(
                    filteredResults, startLat, startLon, endLat, endLon);
            
            // 최대 결과 개수 제한
            List<Place> finalResults = rankedResults.stream()
                    .limit(Math.max(5, maxResults))
                    .collect(Collectors.toList());
            
            // 결과가 없는 경우 대체 데이터 생성
            if (finalResults.isEmpty()) {
                log.warn("No relevant places found after searching, generating fallback places");
                double midLat = (startLat + endLat) / 2;
                double midLon = (startLon + endLon) / 2;
                return generateFallbackPlacesAlongRoute(startLat, startLon, endLat, endLon, placeType, 5);
            }
            
            return finalResults;
        } catch (Exception e) {
            log.error("Error searching places between schedules: {}", e.getMessage(), e);
            // 오류 발생 시 경로 상에 가상 장소 생성
            return generateFallbackPlacesAlongRoute(
                    prevSchedule.getLocation().getLatitude(),
                    prevSchedule.getLocation().getLongitude(),
                    nextSchedule.getLocation().getLatitude(),
                    nextSchedule.getLocation().getLongitude(),
                    placeType, 5);
        }
    }

    /**
     * 기존 위치 기반 검색 (단일 지점)
     */
    public List<Place> searchNearbyPlaces(String placeType, double latitude, double longitude, int radius) {
        try {
            // 여러 검색어 목록 준비
            List<String> searchTerms = getSearchTerms(placeType);
            log.info("Trying search with terms: {}", searchTerms);
            
            List<Place> allResults = new ArrayList<>();
            
            // 각 검색어로 시도
            for (String term : searchTerms) {
                log.info("Searching for '{}' near ({}, {})", term, latitude, longitude);
                
                // 1. Google API로 시도
                List<Place> googleResults = searchNearbyPlacesWithGoogle(term, latitude, longitude, radius);
                if (!googleResults.isEmpty()) {
                    log.info("Found {} places using Google API with term '{}'", googleResults.size(), term);
                    allResults.addAll(googleResults);
                }
                
                // 2. Kakao API로 시도
                List<Place> kakaoResults = searchNearbyPlacesWithKakao(term, latitude, longitude, radius);
                if (!kakaoResults.isEmpty()) {
                    log.info("Found {} places using Kakao API with term '{}'", kakaoResults.size(), term);
                    allResults.addAll(kakaoResults);
                }
                
                // 3. Naver API로 시도
                List<Place> naverResults = searchNearbyPlacesWithNaver(term, latitude, longitude, radius);
                if (!naverResults.isEmpty()) {
                    log.info("Found {} places using Naver API with term '{}'", naverResults.size(), term);
                    allResults.addAll(naverResults);
                }
                
                // 4. Foursquare API로 시도
                List<Place> foursquareResults = searchNearbyPlacesWithFoursquare(term, latitude, longitude, radius);
                if (!foursquareResults.isEmpty()) {
                    log.info("Found {} places using Foursquare API with term '{}'", foursquareResults.size(), term);
                    allResults.addAll(foursquareResults);
                }
            }
            
            // 중복 제거
            List<Place> uniqueResults = removeDuplicates(allResults);
            
            // 관련성 필터링
            List<Place> filteredResults = filterPlacesByRelevance(uniqueResults, placeType);
            
            if (!filteredResults.isEmpty()) {
                return filteredResults;
            }
            
            // 모든 API, 모든 검색어 시도 후에도 결과가 없는 경우
            log.warn("No places found from any API with any search term, generating fallback places");
            return generateFallbackPlaces(latitude, longitude, placeType, radius);
        } catch (Exception e) {
            log.error("Error searching places: {}", e.getMessage(), e);
            return generateFallbackPlaces(latitude, longitude, placeType, radius);
        }
    }
    
    /**
     * 중복 장소 제거 (장소 ID 기준)
     */
    private List<Place> removeDuplicates(List<Place> places) {
        Map<String, Place> uniquePlaces = new HashMap<>();
        
        for (Place place : places) {
            uniquePlaces.putIfAbsent(place.getPlace_id(), place);
        }
        
        return new ArrayList<>(uniquePlaces.values());
    }
    
    /**
     * 장소 유형에 따른 검색어 목록 생성
     */
    private List<String> getSearchTerms(String placeType) {
        List<String> terms = new ArrayList<>();
        terms.add(placeType); // 기본 검색어 추가
        
        // 장소 유형별 대체 검색어
        Map<String, List<String>> alternativeTerms = new HashMap<>();
        alternativeTerms.put("마트", Arrays.asList(
                "대형마트", "슈퍼마켓", "편의점", "grocery", "supermarket", 
                "대형할인점", "홈플러스", "이마트", "롯데마트", "market", "store"));
        alternativeTerms.put("서점", Arrays.asList(
                "책방", "서적", "북스토어", "bookstore", "book", "교보문고", "영풍문고"));
        alternativeTerms.put("카페", Arrays.asList(
                "커피", "coffee", "cafe", "스타벅스", "투썸플레이스", "이디야"));
        alternativeTerms.put("음식점", Arrays.asList(
                "식당", "restaurant", "맛집", "음식"));
        
        // 해당 장소 유형의 대체 검색어 추가
        if (alternativeTerms.containsKey(placeType)) {
            terms.addAll(alternativeTerms.get(placeType));
        }
        
        return terms;
    }
    
    /**
     * 관련성에 따른 장소 필터링
     */
    private List<Place> filterPlacesByRelevance(List<Place> places, String placeType) {
        if (!"마트".equals(placeType)) {
            return places; // 마트가 아닌 경우 필터링 없이 반환
        }
        
        return places.stream()
            .filter(place -> {
                // 메타데이터에서 카테고리 정보 확인
                Map<String, Object> metadata = place.getMetadata();
                Object types = metadata != null ? metadata.get("types") : null;
                
                // 이름 기반 필터링 (마트, 슈퍼, 편의점 등 포함)
                String name = place.getName().toLowerCase();
                boolean nameMatches = name.contains("마트") ||
                    name.contains("슈퍼") ||
                    name.contains("편의점") ||
                    name.contains("이마트") ||
                    name.contains("홈플러스") ||
                    name.contains("롯데마트") ||
                    name.contains("gs") ||
                    name.contains("cu") ||
                    name.contains("세븐일레븐") ||
                    name.contains("market") ||
                    name.contains("store");
                
                // 카테고리 기반 필터링
                boolean categoryMatches = false;
                if (types instanceof List) {
                    categoryMatches = ((List<?>) types).stream()
                        .anyMatch(type -> MART_CATEGORIES.contains(String.valueOf(type)));
                }
                
                // 비즈니스 상태/카테고리 확인
                String businessStatus = place.getBusiness_status();
                boolean businessMatches = businessStatus != null && 
                    (businessStatus.toLowerCase().contains("마트") || 
                     businessStatus.toLowerCase().contains("슈퍼마켓") || 
                     businessStatus.toLowerCase().contains("편의점") ||
                     businessStatus.toLowerCase().contains("쇼핑"));
                
                return nameMatches || categoryMatches || businessMatches;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 경로와의 근접성에 따라 장소 정렬
     */
    private List<Place> rankPlacesByRouteProximity(
            List<Place> places, double startLat, double startLon, double endLat, double endLon) {
        return places.stream()
            .sorted((p1, p2) -> {
                // 장소의 경로와의 거리 계산
                double dist1 = distanceToRoute(
                        p1.getGeometry().getLocation().getLat(),
                        p1.getGeometry().getLocation().getLng(),
                        startLat, startLon, endLat, endLon);
                double dist2 = distanceToRoute(
                        p2.getGeometry().getLocation().getLat(),
                        p2.getGeometry().getLocation().getLng(),
                        startLat, startLon, endLat, endLon);
                
                // 중심점으로부터의 거리
                double mid1 = calculateDistance(
                        p1.getGeometry().getLocation().getLat(),
                        p1.getGeometry().getLocation().getLng(),
                        (startLat + endLat) / 2, 
                        (startLon + endLon) / 2);
                double mid2 = calculateDistance(
                        p2.getGeometry().getLocation().getLat(),
                        p2.getGeometry().getLocation().getLng(),
                        (startLat + endLat) / 2, 
                        (startLon + endLon) / 2);
                
                // 종합 점수 (경로 근접도 + 중심 근접도)
                double score1 = dist1 * 0.7 + mid1 * 0.3;
                double score2 = dist2 * 0.7 + mid2 * 0.3;
                
                return Double.compare(score1, score2);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 경로 상의 여러 지점 샘플링
     */
    private List<Location> samplePointsAlongRoute(
            double startLat, double startLon, double endLat, double endLon, int numPoints) {
        List<Location> points = new ArrayList<>();
        
        // 시작점과 끝점 추가
        points.add(new Location(startLat, startLon, "Start"));
        points.add(new Location(endLat, endLon, "End"));
        
        // 중간 지점 생성
        for (int i = 1; i < numPoints - 1; i++) {
            double fraction = (double) i / (numPoints - 1);
            double lat = startLat + fraction * (endLat - startLat);
            double lon = startLon + fraction * (endLon - startLon);
            points.add(new Location(lat, lon, "Mid" + i));
        }
        
        return points;
    }
    
    /**
     * 점에서 선(경로)까지의 거리 계산
     */
    private double distanceToRoute(
            double pointLat, double pointLon,
            double startLat, double startLon, double endLat, double endLon) {
        // 벡터화
        double[] v = new double[] {endLon - startLon, endLat - startLat};
        double[] w = new double[] {pointLon - startLon, pointLat - startLat};
        
        // 내적
        double c1 = w[0] * v[0] + w[1] * v[1];
        if (c1 <= 0) {
            return calculateDistance(pointLat, pointLon, startLat, startLon);
        }
        
        // 벡터 크기의 제곱
        double c2 = v[0] * v[0] + v[1] * v[1];
        if (c2 <= c1) {
            return calculateDistance(pointLat, pointLon, endLat, endLon);
        }
        
        // 경로 위의 최근접점
        double b = c1 / c2;
        double projLon = startLon + b * v[0];
        double projLat = startLat + b * v[1];
        
        return calculateDistance(pointLat, pointLon, projLat, projLon);
    }
    
    /**
     * 경로를 따라 가상 장소 생성 (대체 데이터)
     */
    private List<Place> generateFallbackPlacesAlongRoute(
            double startLat, double startLon, double endLat, double endLon, 
            String placeType, int numPlaces) {
        List<Place> fallbackPlaces = new ArrayList<>();
        Random random = new Random();
        
        // 경로를 따라 지점 샘플링
        List<Location> routePoints = samplePointsAlongRoute(
                startLat, startLon, endLat, endLon, numPlaces);
        
        // 각 지점 주변에 가상 장소 생성
        for (int i = 0; i < numPlaces; i++) {
            Location point = routePoints.get(i % routePoints.size());
            
            // 약간의 랜덤 오프셋 추가 (±300m)
            double offsetLat = (random.nextDouble() - 0.5) * 0.005; // 약 ±300m
            double offsetLon = (random.nextDouble() - 0.5) * 0.006; // 약 ±300m
            
            double newLat = point.getLatitude() + offsetLat;
            double newLon = point.getLongitude() + offsetLon;
            
            // 장소 정보 생성
            Place place = new Place();
            place.setPlace_id("fallback_route_" + System.currentTimeMillis() + "_" + i);
            
            // 장소 이름 생성
            String placeName = getPlaceName(placeType, i);
            place.setName(placeName);
            
            // 주소 생성
            String address = "울산 ";
            if (newLat > 35.54) {
                address += "북구 ";
            } else if (newLat < 35.52) {
                address += "남구 ";
            } else {
                address += "중구 ";
            }
            address += "가상동 " + (100 + random.nextInt(900)) + "번길 " + (1 + random.nextInt(30));
            place.setFormatted_address(address);
            
            // 위치 설정
            Geometry geometry = new Geometry();
            Geometry.Location location = new Geometry.Location();
            location.setLat(newLat);
            location.setLng(newLon);
            geometry.setLocation(location);
            place.setGeometry(geometry);
            
            // 기타 정보 설정
            place.setRating(3.5 + random.nextDouble() * 1.5); // 3.5~5.0
            place.setUser_ratings_total(10 + random.nextInt(90));
            place.setBusiness_status("OPERATIONAL");
            place.setOpen_now(true);
            
            // 메타데이터
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("distance", calculateDistance(
                    startLat, startLon, newLat, newLon));
            metadata.put("crowdLevel", 0.3 + random.nextDouble() * 0.5); // 0.3~0.8
            metadata.put("isOpen", true);
            metadata.put("source", "fallback");
            
            place.setMetadata(metadata);
            fallbackPlaces.add(place);
        }
        
        log.info("Generated {} fallback places along route", fallbackPlaces.size());
        return fallbackPlaces;
    }

    /**
     * 구글 Places API를 사용한 장소 검색
     */
    public List<Place> searchNearbyPlacesWithGoogle(String placeType, double latitude, double longitude, int radius) {
        try {
            log.info("Searching for '{}' near ({}, {}) with Google Places API, radius {}m", 
                    placeType, latitude, longitude, radius);
            
            // 검색어 인코딩
            String encodedType = URLEncoder.encode(placeType, StandardCharsets.UTF_8.toString());
            
            // Google Places API URL 구성
            String googleApiUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(googleApiUrl)
                .queryParam("keyword", encodedType)
                .queryParam("location", latitude + "," + longitude)
                .queryParam("radius", radius)
                .queryParam("language", "ko") // 한국어 결과
                .queryParam("key", googleApiKey);
            
            String requestUrl = builder.toUriString();
            log.info("Google API request URL: {}", requestUrl);
            
            ResponseEntity<String> response;
            try {
                response = restTemplate.getForEntity(requestUrl, String.class);
                log.info("Google API response status: {}", response.getStatusCode());
                log.info("Response body preview: {}", response.getBody().substring(0, 
                        Math.min(response.getBody().length(), 500)));
            } catch (Exception e) {
                log.error("Error calling Google API: {}", e.getMessage());
                return Collections.emptyList();
            }
            
            // JSON 응답 처리
            List<Place> googlePlaces = new ArrayList<>();
            JsonNode root = objectMapper.readTree(response.getBody());
            
            if (!root.path("status").asText().equals("OK") && !root.path("status").asText().equals("ZERO_RESULTS")) {
                log.warn("Google API returned error status: {}", root.path("status").asText());
                if (root.has("error_message")) {
                    log.warn("Error message: {}", root.path("error_message").asText());
                }
                return Collections.emptyList();
            }
            
            if (root.has("results") && root.get("results").isArray()) {
                JsonNode resultsNode = root.get("results");
                log.info("Google API returned {} results", resultsNode.size());
                
                for (JsonNode result : resultsNode) {
                    try {
                        Place place = new Place();
                        place.setPlace_id(result.get("place_id").asText());
                        place.setName(result.get("name").asText());
                        
                        // 주소 설정
                        place.setFormatted_address(
                            result.has("vicinity") ? result.get("vicinity").asText() : ""
                        );
                        
                        // 위치 설정
                        JsonNode location = result.get("geometry").get("location");
                        Geometry geometry = new Geometry();
                        Geometry.Location loc = new Geometry.Location();
                        loc.setLat(location.get("lat").asDouble());
                        loc.setLng(location.get("lng").asDouble());
                        geometry.setLocation(loc);
                        place.setGeometry(geometry);
                        
                        // 평점 설정
                        if (result.has("rating")) {
                            place.setRating(result.get("rating").asDouble());
                        } else {
                            place.setRating(4.0);
                        }
                        
                        // 영업 상태 설정
                        if (result.has("business_status")) {
                            place.setBusiness_status(result.get("business_status").asText());
                        }
                        
                        // 영업 중 상태 설정
                        if (result.has("opening_hours") && result.get("opening_hours").has("open_now")) {
                            place.setOpen_now(result.get("opening_hours").get("open_now").asBoolean());
                        } else {
                            place.setOpen_now(true);
                        }
                        
                        // 메타데이터 설정
                        Map<String, Object> metadata = new HashMap<>();
                        // 거리 계산 (직선 거리)
                        double distance = calculateDistance(
                            latitude, longitude, 
                            loc.getLat(), loc.getLng()
                        );
                        metadata.put("distance", distance);
                        
                        // 장소 유형 정보
                        if (result.has("types") && result.get("types").isArray()) {
                            List<String> types = new ArrayList<>();
                            for (JsonNode type : result.get("types")) {
                                types.add(type.asText());
                            }
                            metadata.put("types", types);
                        }
                        
                        place.setMetadata(metadata);
                        googlePlaces.add(place);
                    } catch (Exception e) {
                        log.warn("Error parsing Google place result: {}", e.getMessage());
                    }
                }
            }
            
            if (googlePlaces.isEmpty()) {
                log.warn("No places found from Google API");
            }
            
            return googlePlaces;
        } catch (Exception e) {
            log.error("Error searching places with Google API: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     *
/**
     * 카카오 API를 사용한 장소 검색
     */
    public List<Place> searchNearbyPlacesWithKakao(String placeType, double latitude, double longitude, int radius) {
        try {
            log.info("Searching for '{}' near ({}, {}) with Kakao API, radius {}m", 
                    placeType, latitude, longitude, radius);
            
            // 인코딩 문제 해결을 위한 처리 (URL 인코딩)
            String normalizedType;
            try {
                normalizedType = URLEncoder.encode(placeType, StandardCharsets.UTF_8.toString());
                log.info("Encoded search term: {}", normalizedType);
            } catch (Exception e) {
                log.warn("Failed to encode search term, using as-is: {}", placeType);
                normalizedType = placeType;
            }
            
            // Kakao Local API 호출
            String kakaoApiUrl = "https://dapi.kakao.com/v2/local/search/keyword.json";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(kakaoApiUrl)
                .queryParam("query", normalizedType)
                .queryParam("x", longitude)
                .queryParam("y", latitude)
                .queryParam("radius", radius)
                .queryParam("size", 15);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoApiKey);

            ResponseEntity<String> response;
            try {
                response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
                );
                
                // API 응답 로깅
                log.info("API response status: {}", response.getStatusCode());
                log.info("Response body preview: {}", response.getBody().substring(0, 
                        Math.min(response.getBody().length(), 500)));
            } catch (Exception e) {
                log.error("Kakao API call failed: {}", e.getMessage());
                return Collections.emptyList();
            }

            // JSON 응답 처리
            List<Place> kakaoPlaces = new ArrayList<>();
            try {
                JsonNode root = objectMapper.readTree(response.getBody());
                
                if (root.has("documents") && root.get("documents").isArray()) {
                    JsonNode documents = root.get("documents");
                    log.info("API returned {} results", documents.size());
                    
                    for (JsonNode document : documents) {
                        Place place = new Place();
                        place.setPlace_id(document.get("id").asText());
                        place.setName(document.get("place_name").asText());
                        place.setFormatted_address(document.get("address_name").asText());
                        
                        // Geometry 설정
                        Geometry geometry = new Geometry();
                        Geometry.Location location = new Geometry.Location();
                        location.setLat(document.get("y").asDouble());
                        location.setLng(document.get("x").asDouble());
                        geometry.setLocation(location);
                        place.setGeometry(geometry);
                        
                        place.setRating(document.has("rating") ? document.get("rating").asDouble() : 4.0);
                        place.setBusiness_status(document.has("category_name") ? document.get("category_name").asText() : "");
                        place.setOpen_now(true); // 기본값, 실제로는 추가 API 호출로 확인 필요
                        
                        // 메타데이터
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("distance", document.get("distance").asDouble());
                        metadata.put("placeUrl", document.get("place_url").asText());
                        
                        // 혼잡도, 영업시간 등 추가 정보 (별도 API 호출 필요할 수 있음)
                        Map<String, Object> details = getPlaceDetails(document.get("id").asText());
                        metadata.putAll(details);
                        
                        place.setMetadata(metadata);
                        kakaoPlaces.add(place);
                    }
                } else {
                    log.warn("API response has no documents array or is empty");
                }
            } catch (Exception e) {
                log.error("Error parsing API response: {}", e.getMessage());
            }
            
            return kakaoPlaces;
        } catch (Exception e) {
            log.error("Error searching places with Kakao API: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 네이버 지역 검색 API를 사용한 장소 검색
     */
    public List<Place> searchNearbyPlacesWithNaver(String placeType, double latitude, double longitude, int radius) {
        try {
            log.info("Searching for '{}' near ({}, {}) with Naver API, radius {}m", 
                    placeType, latitude, longitude, radius);
            
            // 검색어 인코딩
            String encodedQuery;
            try {
                encodedQuery = URLEncoder.encode(placeType, StandardCharsets.UTF_8.toString());
            } catch (Exception e) {
                log.warn("Failed to encode query for Naver API, using as-is: {}", placeType);
                encodedQuery = placeType;
            }
            
            // 네이버 지역 검색 API 설정
            String naverSearchUrl = "https://openapi.naver.com/v1/search/local.json";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(naverSearchUrl)
                .queryParam("query", encodedQuery)
                .queryParam("display", 5) // 결과 개수
                .queryParam("start", 1)   // 시작 위치
                .queryParam("sort", "random"); // 정렬 (random은 무작위)
            
            // API 호출을 위한 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Naver-Client-Id", naverClientId);
            headers.set("X-Naver-Client-Secret", naverClientSecret);
            
            ResponseEntity<String> response;
            try {
                response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
                );
                
                log.info("Naver API response status: {}", response.getStatusCode());
                log.info("Response body preview: {}", response.getBody().substring(0, 
                        Math.min(response.getBody().length(), 500)));
            } catch (Exception e) {
                log.error("Naver API call failed: {}", e.getMessage());
                return Collections.emptyList();
            }
            
            // 응답 처리
            List<Place> naverPlaces = new ArrayList<>();
            try {
                JsonNode root = objectMapper.readTree(response.getBody());
                
                if (root.has("items") && root.get("items").isArray()) {
                    JsonNode items = root.get("items");
                    log.info("Naver API returned {} results", items.size());
                    
                    for (JsonNode item : items) {
                        // 거리 필터링 - 직선 거리 계산 
                        double mapx = Double.parseDouble(item.get("mapx").asText()) / 10000000.0;
                        double mapy = Double.parseDouble(item.get("mapy").asText()) / 10000000.0;
                        double distance = calculateDistance(latitude, longitude, mapy, mapx);
                        
                        // 검색 반경 내의 장소만 포함
                        if (distance > radius) {
                            continue;
                        }
                        
                        Place place = new Place();
                        place.setPlace_id("naver_" + System.currentTimeMillis() + "_" + naverPlaces.size());
                        
                        // HTML 태그 제거
                        String title = item.get("title").asText();
                        title = title.replaceAll("<[^>]*>", "");
                        place.setName(title);
                        
                        // 주소 설정 (도로명 주소 우선, 없으면 일반 주소)
                        String roadAddress = item.has("roadAddress") ? item.get("roadAddress").asText() : "";
                        String address = item.has("address") ? item.get("address").asText() : "";
                        place.setFormatted_address(!roadAddress.isEmpty() ? roadAddress : address);
                        
                        // 좌표 설정
                        Geometry geometry = new Geometry();
                        Geometry.Location location = new Geometry.Location();
                        location.setLat(mapy);
                        location.setLng(mapx);
                        geometry.setLocation(location);
                        place.setGeometry(geometry);
                        
                        // 기타 정보 설정
                        place.setRating(4.0);  // 네이버 API는 평점 제공 안함
                        place.setBusiness_status(item.has("category") ? item.get("category").asText() : "");
                        place.setOpen_now(true);
                        
                        // 메타데이터 
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("distance", distance);
                        if (item.has("telephone")) {
                            metadata.put("telephone", item.get("telephone").asText());
                        }
                        metadata.put("source", "naver");
                        
                        place.setMetadata(metadata);
                        naverPlaces.add(place);
                    }
                }
            } catch (Exception e) {
                log.error("Error parsing Naver API response: {}", e.getMessage());
            }
            
            return naverPlaces;
        } catch (Exception e) {
            log.error("Error searching with Naver API: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Foursquare API를 사용한 장소 검색
     */
    public List<Place> searchNearbyPlacesWithFoursquare(String placeType, double latitude, double longitude, int radius) {
        try {
            log.info("Searching for '{}' near ({}, {}) with Foursquare API, radius {}m", 
                    placeType, latitude, longitude, radius);
            
            // Foursquare Places API URL
            String foursquareApiUrl = "https://api.foursquare.com/v3/places/search";
            
            // 카테고리 매핑 (필요한 경우)
            Map<String, String> categoryMapping = new HashMap<>();
            categoryMapping.put("마트", "17000"); // 상점 및 서비스
            categoryMapping.put("서점", "17096");  // 서점
            categoryMapping.put("카페", "13032");  // 카페
            categoryMapping.put("음식점", "13000"); // 음식
            
            String categoryId = categoryMapping.getOrDefault(placeType, "");
            
            // API 요청 구성
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(foursquareApiUrl)
                .queryParam("query", placeType)
                .queryParam("ll", latitude + "," + longitude)
                .queryParam("radius", radius)
                .queryParam("limit", 10);
                
            if (!categoryId.isEmpty()) {
                builder.queryParam("categories", categoryId);
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.set("Authorization", foursquareApiKey);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response;
            try {
                response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    String.class
                );
                
                log.info("Foursquare API response status: {}", response.getStatusCode());
                log.info("Response body preview: {}", response.getBody().substring(0, 
                        Math.min(response.getBody().length(), 500)));
            } catch (Exception e) {
                log.error("Foursquare API call failed: {}", e.getMessage());
                return Collections.emptyList();
            }
            
            // 응답 파싱
            List<Place> foursquarePlaces = new ArrayList<>();
            try {
                JsonNode root = objectMapper.readTree(response.getBody());
                
                if (root.has("results") && root.get("results").isArray()) {
                    JsonNode results = root.get("results");
                    log.info("Foursquare API returned {} results", results.size());
                    
                    for (JsonNode result : results) {
                        try {
                            Place place = new Place();
                            place.setPlace_id(result.path("fsq_id").asText());
                            place.setName(result.path("name").asText());
                            
                            // 주소 설정
                            JsonNode location = result.path("location");
                            StringBuilder addressBuilder = new StringBuilder();
                            
                            if (location.has("formatted_address")) {
                                addressBuilder.append(location.get("formatted_address").asText());
                            } else {
                                if (location.has("address")) {
                                    addressBuilder.append(location.get("address").asText());
                                }
                                if (location.has("locality")) {
                                    if (addressBuilder.length() > 0) addressBuilder.append(", ");
                                    addressBuilder.append(location.get("locality").asText());
                                }
                                if (location.has("region")) {
                                    if (addressBuilder.length() > 0) addressBuilder.append(", ");
                                    addressBuilder.append(location.get("region").asText());
                                }
                            }
                            
                            place.setFormatted_address(addressBuilder.toString());
                            
                            // 좌표 설정
                            JsonNode geocodes = result.path("geocodes");
                            if (geocodes.has("main")) {
                                JsonNode main = geocodes.get("main");
                                Geometry geometry = new Geometry();
                                Geometry.Location loc = new Geometry.Location();
                                loc.setLat(main.path("latitude").asDouble());
                                loc.setLng(main.path("longitude").asDouble());
                                geometry.setLocation(loc);
                                place.setGeometry(geometry);
                            }
                            
                            // 평점 설정 (Foursquare API에서는 rating이 없을 수 있음)
                            place.setRating(4.0);
                            
                            // 카테고리 설정
                            JsonNode categories = result.path("categories");
                            if (categories.isArray() && categories.size() > 0) {
                                place.setBusiness_status(categories.get(0).path("name").asText());
                            }
                            
                            // 영업 여부 (기본값으로 설정)
                            place.setOpen_now(true);
                            
                            // 메타데이터
                            Map<String, Object> metadata = new HashMap<>();
                            
                            // 거리 계산
                            if (place.getGeometry() != null && place.getGeometry().getLocation() != null) {
                                double placeLat = place.getGeometry().getLocation().getLat();
                                double placeLng = place.getGeometry().getLocation().getLng();
                                metadata.put("distance", calculateDistance(latitude, longitude, placeLat, placeLng));
                            } else if (location.has("distance")) {
                                metadata.put("distance", location.path("distance").asDouble());
                            }
                            
                            metadata.put("source", "foursquare");
                            
                            // 카테고리 정보 추가
                            if (categories.isArray()) {
                                List<String> categoryNames = new ArrayList<>();
                                for (JsonNode category : categories) {
                                    categoryNames.add(category.path("name").asText());
                                }
                                metadata.put("categories", categoryNames);
                            }
                            
                            place.setMetadata(metadata);
                            foursquarePlaces.add(place);
                        } catch (Exception e) {
                            log.warn("Error parsing Foursquare place: {}", e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error parsing Foursquare API response: {}", e.getMessage());
            }
            
            return foursquarePlaces;
        } catch (Exception e) {
            log.error("Error searching with Foursquare API: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private Map<String, Object> getPlaceDetails(String placeId) {
        // 장소 상세 정보 가져오기 (영업시간, 혼잡도 등)
        // 실제로는 Kakao Place Detail API 호출
        Map<String, Object> details = new HashMap<>();
        details.put("isOpen", true); // 예시
        details.put("crowdLevel", 0.3); // 예시
        return details;
    }

    /**
     * 임시 장소 데이터 생성
     * API 호출 실패 또는 결과가 없을 때 테스트용 데이터 제공
     */
    private List<Place> generateFallbackPlaces(double latitude, double longitude, String placeType, int radius) {
        List<Place> fallbackPlaces = new ArrayList<>();
        Random random = new Random();
        
        // 주변에 가상의 위치 생성 (4방향)
        double[] offsetsLat = {0.005, -0.005, 0.01, -0.01}; // 약 500m, 1km 간격
        double[] offsetsLon = {0.008, -0.008, 0.015, -0.015}; // 경도는 위도보다 간격을 넓게
        
        int placeCount = Math.min(4, random.nextInt(3) + 2); // 2~4개의 장소 생성
        
        for (int i = 0; i < placeCount; i++) {
            // 랜덤한 방향으로 위치 오프셋 적용
            double offsetLat = offsetsLat[i % offsetsLat.length] * (0.5 + random.nextDouble());
            double offsetLon = offsetsLon[i % offsetsLon.length] * (0.5 + random.nextDouble());
            
            double newLat = latitude + offsetLat;
            double newLon = longitude + offsetLon;
            
            // 거리 계산 (대략적인 미터 단위)
            double distanceMeters = Math.sqrt(Math.pow(offsetLat * 111000, 2) + Math.pow(offsetLon * 111000 * Math.cos(Math.toRadians(latitude)), 2));
            
            Place place = new Place();
            place.setPlace_id("fallback_" + System.currentTimeMillis() + "_" + i);
            
            // 장소 이름 생성
            String placeName = getPlaceName(placeType, i);
            place.setName(placeName);
            
            // 주소 생성
            String address = "울산 ";
            if (latitude > 35.54) {
                address += "북구 ";
            } else if (latitude < 35.52) {
                address += "남구 ";
            } else {
                address += "중구 ";
            }
            address += "가상동 " + (100 + random.nextInt(900)) + "번길 " + (1 + random.nextInt(30));
            place.setFormatted_address(address);
            
            // 위치 설정
            Geometry geometry = new Geometry();
            Geometry.Location location = new Geometry.Location();
            location.setLat(newLat);
            location.setLng(newLon);
            geometry.setLocation(location);
            place.setGeometry(geometry);
            
            // 기타 정보 설정
            place.setRating(3.5 + random.nextDouble() * 1.5); // 3.5~5.0
            place.setUser_ratings_total(10 + random.nextInt(90));
            place.setBusiness_status("OPERATIONAL");
            place.setOpen_now(true);
            
            // 메타데이터
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("distance", distanceMeters);
            metadata.put("crowdLevel", 0.3 + random.nextDouble() * 0.5); // 0.3~0.8
            metadata.put("isOpen", true);
            metadata.put("source", "fallback");
            
            place.setMetadata(metadata);
            fallbackPlaces.add(place);
        }
        
        log.info("Generated {} fallback places for testing", fallbackPlaces.size());
        return fallbackPlaces;
    }

    /**
     * 장소 유형에 따른 적절한 이름 생성
     */
    private String getPlaceName(String placeType, int index) {
        Map<String, List<String>> namesByType = new HashMap<>();
        namesByType.put("마트", Arrays.asList("굿모닝마트", "이마트", "홈플러스", "롯데마트", "코스트코"));
        namesByType.put("서점", Arrays.asList("교보문고", "영풍문고", "반디앤루니스", "알라딘"));
        namesByType.put("카페", Arrays.asList("스타벅스", "투썸플레이스", "이디야커피", "빽다방"));
        namesByType.put("음식점", Arrays.asList("맛있는 식당", "행복한 밥상", "어머니 손맛", "가족 식당"));
        namesByType.put("슈퍼마켓", Arrays.asList("GS슈퍼마켓", "신선마트", "동네슈퍼", "새마을슈퍼"));
        namesByType.put("편의점", Arrays.asList("GS25", "CU", "세븐일레븐", "이마트24"));
        
        List<String> names = namesByType.getOrDefault(placeType, Arrays.asList(placeType + " 장소"));
        return names.get(index % names.size()) + " " + (index + 1) + "호점";
    }

    // 거리 계산 유틸리티 메서드
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // 지구 반경(km)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = 
            Math.sin(dLat/2) * Math.sin(dLat/2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c * 1000; // 미터 단위로 변환
    }
    
    // 캐시 설정
    private final Map<String, PlaceInfo> placeInfoCache = new ConcurrentHashMap<>();
    private final Map<String, TrafficInfo> trafficInfoCache = new ConcurrentHashMap<>();
    
    public Map<String, Object> getIntegratedLocationInfo(String placeName, Location location, LocalDateTime time) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. LocationInfoService를 통한 통합 장소 정보 조회
            PlaceInfo placeInfo = locationInfoService.getPlaceInfo(placeName);
            
            if (placeInfo != null) {
                // 2. 실시간 혼잡도 분석 (시간대 기반)
                double crowdLevel = crowdLevelAnalyzer.analyzeCrowdLevel(time);
                
                // 3. 기본 정보 설정
                result.put("name", placeInfo.getName());
                result.put("location", location);
                result.put("id", placeInfo.getId());
                result.put("crowdLevel", Math.max(crowdLevel, placeInfo.getCrowdLevel()));
                result.put("category", placeInfo.getCategory());
                result.put("rating", placeInfo.getRating());
                result.put("phoneNumber", placeInfo.getPhoneNumber());
                result.put("address", placeInfo.getAddress());
                
                // 4. 영업시간 정보 처리
                result.put("isOpen", placeInfo.isOpen());
                result.put("operatingHours", Map.of(
                    "open", placeInfo.getOpenTime().toString(),
                    "close", placeInfo.getCloseTime().toString()
                ));
                
                // 5. 방문 추천 정보 생성
                Map<String, Object> recommendation = new HashMap<>();
                recommendation.put("bestVisitTime", determineBestVisitTime(placeInfo, crowdLevel));
                recommendation.put("estimatedDuration", placeInfo.getAverageVisitDuration().toString());
                recommendation.put("crowdLevelStatus", getCrowdLevelStatus(crowdLevel));
                result.put("recommendation", recommendation);
            } else {
                log.warn("No place information found for: {}", placeName);
                result.put("error", "Place information not found");
            }
            
        } catch (Exception e) {
            log.error("Error getting integrated location info for {}: {}", placeName, e.getMessage());
            result.put("error", "Failed to get complete location information");
        }
        
        return result;
    }
    

    public TrafficInfo getIntegratedTrafficInfo(Location start, Location end, LocalDateTime time) {
        String cacheKey = String.format("%f,%f-%f,%f-%s",
            start.getLatitude(), start.getLongitude(),
            end.getLatitude(), end.getLongitude(),
            time.toString()
        );
        
        try {
            // 캐시된 정보 확인 및 API 호출
            return trafficInfoCache.computeIfAbsent(cacheKey, key -> {
                // T Map API를 통한 교통 정보 조회
                TrafficInfo trafficInfo = firstMapService.getTrafficInfo(start, end);
                
                // 시간대별 가중치 적용
                double timeBasedFactor = getTimeBasedTrafficFactor(time);
                
                // 보정된 교통 정보 반환
                return new TrafficInfo(
                    trafficInfo.getTrafficRate() * timeBasedFactor,
                    (int)(trafficInfo.getEstimatedTime() * timeBasedFactor),
                    trafficInfo.getDistance()
                );
            });
            
        } catch (Exception e) {
            log.error("Error getting traffic info: {}", e.getMessage());
            // 기본값 반환
            return new TrafficInfo(1.0, 30, 5.0);
        }
    }

    private String determineBestVisitTime(PlaceInfo placeInfo, double crowdLevel) {
        LocalTime now = LocalTime.now();
        
        // 현재 영업 시간이 아닌 경우
        if (!isWithinOperatingHours(now, placeInfo)) {
            return String.format("영업시간(%s-%s) 중 방문 권장", 
                placeInfo.getOpenTime(), placeInfo.getCloseTime());
        }
        
        // 혼잡도 기반 추천
        if (crowdLevel > 0.8) {
            return "현재 혼잡, 다른 시간대 방문 권장";
        } else if (crowdLevel > 0.5) {
            return "보통 수준의 혼잡도, 방문 가능";
        } else {
            return "현재 방문하기 좋은 시간";
        }
    }

    private boolean isWithinOperatingHours(LocalTime time, PlaceInfo placeInfo) {
        return !time.isBefore(placeInfo.getOpenTime()) && 
               !time.isAfter(placeInfo.getCloseTime());
    }
    
    
    private String getCrowdLevelStatus(double crowdLevel) {
        if (crowdLevel < 0.3) return "여유";
        if (crowdLevel < 0.6) return "보통";
        if (crowdLevel < 0.8) return "혼잡";
        return "매우 혼잡";
    }
    
    private double calculateTrafficRate(JsonNode trafficJson) {
        // 교통 정보에서 평균 혼잡도 계산
        JsonNode trafficInfo = trafficJson.path("trafficInfo");
        if (trafficInfo.isArray()) {
            double totalRate = 0;
            int count = 0;
            
            for (JsonNode info : trafficInfo) {
                if (info.has("congestion")) {
                    totalRate += info.path("congestion").asDouble();
                    count++;
                }
            }
            
            return count > 0 ? (totalRate / count) : 1.0;
        }
        return 1.0; // 기본값
    }

private double getTimeBasedTrafficFactor(LocalDateTime time) {
        int hour = time.getHour();
        
        // 출퇴근 시간대
        if ((hour >= 8 && hour <= 10) || (hour >= 18 && hour <= 20)) {
            return 1.5; // 50% 가중치
        }
        
        // 점심 시간대
        if (hour >= 12 && hour <= 14) {
            return 1.3; // 30% 가중치
        }
        
        // 그 외 시간대
        return 1.0;
    }
}