package com.example.TripSpring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
public class KakaoLocalService {
    private static final String LOCAL_SEARCH_API_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";
    @Value("${app.api.kakao}")
    private String KAKAO_API_KEY;

    public Map<String, Object> searchPlace(String query) {
        try {
            // 검색어 URL 인코딩
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            
            // API 요청 URL 생성
            String apiUrl = String.format("%s?query=%s", LOCAL_SEARCH_API_URL, encodedQuery);

            // HTTP 연결 설정
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "KakaoAK " + KAKAO_API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");

            // 응답 코드 확인
            int responseCode = conn.getResponseCode();
            
            // 응답 읽기
            BufferedReader br;
            if(responseCode >= 200 && responseCode <= 300) {
                br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            } else {
                br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            }

            // 응답 본문 읽기
            StringBuilder responseBody = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                responseBody.append(line);
            }
            br.close();
            conn.disconnect();

            // JSON 파싱
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> result = mapper.readValue(responseBody.toString(), Map.class);

            // 로깅
            log.info("카카오 로컬 API 응답 상태: {}", responseCode);
            log.info("카카오 로컬 API 응답 본문: {}", responseBody);

            return processSearchResult(result);

        } catch (Exception e) {
            log.error("장소 검색 중 오류 발생", e);
            return Map.of(
                "status", "ERROR", 
                "message", "장소 검색 중 오류 발생: " + e.getMessage()
            );
        }
    }

    private Map<String, Object> processSearchResult(Map<String, Object> result) {
        Map<String, Object> processedResult = new HashMap<>();
        
        try {
            // 검색 결과 확인
            Object documentsObj = result.get("documents");
            if (documentsObj instanceof java.util.List && !((java.util.List<?>) documentsObj).isEmpty()) {
                Map<String, Object> firstPlace = (Map<String, Object>) ((java.util.List<?>) documentsObj).get(0);

                processedResult.put("status", "SUCCESS");
                processedResult.put("name", firstPlace.get("place_name"));
                processedResult.put("address", firstPlace.get("address_name"));
                processedResult.put("roadAddress", firstPlace.get("road_address_name"));
                processedResult.put("latitude", firstPlace.get("y"));
                processedResult.put("longitude", firstPlace.get("x"));
                processedResult.put("category", firstPlace.get("category_name"));
                processedResult.put("phone", firstPlace.get("phone"));
            } else {
                processedResult.put("status", "NO_RESULTS");
                processedResult.put("message", "검색 결과가 없습니다.");
            }
        } catch (Exception e) {
            log.error("결과 처리 중 오류 발생", e);
            processedResult.put("status", "ERROR");
            processedResult.put("message", "결과 처리 중 오류 발생: " + e.getMessage());
        }

        return processedResult;
    }
}