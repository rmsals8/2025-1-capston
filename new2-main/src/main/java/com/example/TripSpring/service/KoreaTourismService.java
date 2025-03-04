package com.example.TripSpring.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.json.JSONObject;
import org.json.XML;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class KoreaTourismService {
    private final RestTemplate restTemplate;
    private final String BASE_URL = "http://apis.data.go.kr/B551011/KorService1";
    @Value("${app.api.tourism}")
    private String API_KEY;

    @Autowired
    public KoreaTourismService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
    }

    private String processResponse(String response) {
        if (response.trim().startsWith("<")) {
            // XML 응답을 JSON으로 변환
            JSONObject jsonObj = XML.toJSONObject(response);
            return jsonObj.toString();
        }
        return response; // 이미 JSON이면 그대로 반환
    }

    @SuppressWarnings("null")
    public String searchTouristSpots(String keyword, int pageNo, int numOfRows) {
        try {
            String encodedKey = URLEncoder.encode(API_KEY, StandardCharsets.UTF_8);
            String directUrl = BASE_URL + "/searchKeyword1"
                + "?serviceKey=" + encodedKey
                + "&numOfRows=" + numOfRows
                + "&pageNo=" + pageNo
                + "&MobileOS=ETC"
                + "&MobileApp=TripSpring"
                + "&_type=json"
                + "&keyword=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8);

            int maxRetries = 3;
            int retryCount = 0;
            while (retryCount < maxRetries) {
                try {
                    String response = restTemplate.getForObject(directUrl, String.class);
                    if (!response.contains("SERVICE_KEY_IS_NOT_REGISTERED_ERROR")) {
                        return processResponse(response);
                    }
                    retryCount++;
                    Thread.sleep(1000);
                } catch (Exception e) {
                    retryCount++;
                    if (retryCount == maxRetries) throw e;
                    Thread.sleep(1000);
                }
            }
            throw new RuntimeException("Failed after " + maxRetries + " retries");
        } catch (Exception e) {
            log.error("Failed to search tourist spots", e);
            throw new RuntimeException("Failed to search tourist spots: " + e.getMessage());
        }
    }

    @SuppressWarnings("null")
    public String getSpotDetails(String contentId, String contentTypeId) {
        try {
            String encodedKey = URLEncoder.encode(API_KEY, StandardCharsets.UTF_8);
            String directUrl = BASE_URL + "/detailCommon1"
                + "?serviceKey=" + encodedKey
                + "&contentId=" + contentId
                + "&contentTypeId=" + contentTypeId
                + "&MobileOS=ETC"
                + "&MobileApp=TripSpring"
                + "&_type=json"
                + "&defaultYN=Y"
                + "&firstImageYN=Y"
                + "&addrinfoYN=Y"
                + "&mapinfoYN=Y"
                + "&overviewYN=Y";

            int maxRetries = 3;
            int retryCount = 0;
            while (retryCount < maxRetries) {
                try {
                    String response = restTemplate.getForObject(directUrl, String.class);
                    if (!response.contains("SERVICE_KEY_IS_NOT_REGISTERED_ERROR")) {
                        return processResponse(response);
                    }
                    retryCount++;
                    Thread.sleep(1000);
                } catch (Exception e) {
                    retryCount++;
                    if (retryCount == maxRetries) throw e;
                    Thread.sleep(1000);
                }
            }
            throw new RuntimeException("Failed after " + maxRetries + " retries");
        } catch (Exception e) {
            log.error("Failed to get spot details", e);
            throw new RuntimeException("Failed to get spot details: " + e.getMessage());
        }
    }

    @SuppressWarnings("null")
    public String getAreaBasedList(String areaCode, String sigunguCode) {
        try {
            String encodedKey = URLEncoder.encode(API_KEY, StandardCharsets.UTF_8);
            String directUrl = BASE_URL + "/areaBasedList1"
                + "?serviceKey=" + encodedKey
                + "&numOfRows=10"
                + "&pageNo=1"
                + "&MobileOS=ETC"
                + "&MobileApp=TripSpring"
                + "&_type=json"
                + "&areaCode=" + areaCode;

            if (sigunguCode != null && !sigunguCode.isEmpty()) {
                directUrl += "&sigunguCode=" + sigunguCode;
            }

            int maxRetries = 10;
            int retryCount = 0;
            while (retryCount < maxRetries) {
                try {
                    String response = restTemplate.getForObject(directUrl, String.class);
                    if (!response.contains("SERVICE_KEY_IS_NOT_REGISTERED_ERROR")) {
                        return processResponse(response);
                    }
                    retryCount++;
                    Thread.sleep(5000);
                } catch (Exception e) {
                    retryCount++;
                    if (retryCount == maxRetries) throw e;
                    Thread.sleep(5000);
                }
            }
            throw new RuntimeException("Failed after " + maxRetries + " retries");
        } catch (Exception e) {
            log.error("Failed to get area based list", e);
            throw new RuntimeException("Failed to get area based list: " + e.getMessage());
        }
    }
}