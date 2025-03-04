package com.example.TripSpring.service;
import com.example.TripSpring.dto.request.ScheduleOptimizationRequest;
import com.example.TripSpring.dto.request.ScheduleOptimizationRequest.LocationDto;
import com.example.TripSpring.dto.request.ScheduleOptimizationRequest.ScheduleDto;
import com.example.TripSpring.domain.InputType;
import com.example.TripSpring.domain.ScheduleData;
import com.example.TripSpring.domain.ScheduleType;
import com.example.TripSpring.exception.ScheduleProcessException;
import com.example.TripSpring.util.GptResponseParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleProcessorService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final GptResponseParser gptResponseParser;    
    private final NaverPlaceSearchService placeSearchService;
    
    private RetryTemplate retryTemplate;
    
    @Value("${app.api.openai}")
    private String apiKey;
    private String apiUrl = "https://api.openai.com/v1/chat/completions";

    @PostConstruct
    public void init() {
        RetryPolicy retryPolicy = new SimpleRetryPolicy(3);
        
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(2000);
        
        this.retryTemplate = new RetryTemplate();
        this.retryTemplate.setRetryPolicy(retryPolicy);
        this.retryTemplate.setBackOffPolicy(backOffPolicy);
    }

    public ScheduleData processInput(String input, InputType type) {
        try {
            String processedInput = preprocessInput(input, type);
            String classification = classifySchedule(processedInput);
            Map<String, Object> details = extractScheduleDetails(processedInput, classification);
            return convertToScheduleData(details);
        } catch (Exception e) {
            log.error("Error processing input: {}", e.getMessage(), e);
            throw new ScheduleProcessException("Failed to process input", e);
        }
    }

    private String preprocessInput(String input, InputType type) {
        if (type == InputType.VOICE) {
            return input.trim().toLowerCase()
                    .replaceAll("\\s+", " ")
                    .replaceAll("어[,.]?\\s*", "")
                    .replaceAll("음[,.]?\\s*", "");
        }
        return input.trim();
    }

    private String classifySchedule(String input) {
        String prompt = createClassificationPrompt(input);
        return callGptApi(prompt);
    }

    private Map<String, Object> extractScheduleDetails(String input, String classification) {
        String prompt = createExtractionPrompt(input, classification);
        String response = callGptApi(prompt);
        
        try {
            Map<String, Object> details = objectMapper.readValue(response, Map.class);
            validateScheduleDetails(details);
            return details;
        } catch (Exception e) {
            log.error("Failed to parse schedule details: {}", response, e);
            throw new ScheduleProcessException("Failed to parse schedule details", e);
        }
    }

    private void validateScheduleDetails(Map<String, Object> details) {
        if (details == null) {
            throw new ScheduleProcessException("Schedule details cannot be null");
        }
        
        String[] requiredFields = {"location", "time", "priority", "type"};
        for (String field : requiredFields) {
            if (!details.containsKey(field) || details.get(field) == null) {
                throw new ScheduleProcessException("Missing required field: " + field);
            }
        }
    }

    private String createClassificationPrompt(String input) {
        return String.format("""
            다음 일정을 '고정(FIXED)' 또는 '유동(FLEXIBLE)'으로 분류해주세요:
            입력: %s
            출력 형식: FIXED 또는 FLEXIBLE
            """, input);
    }

    private String createExtractionPrompt(String input, String classification) {
        return String.format("""
            다음 일정에서 장소, 시간, 우선순위를 추출해서 JSON 형식으로 응답해주세요:
            입력: %s
            분류: %s
            
            응답 예시:
            {
                "location": "강남역",
                "time": "2024-02-02 14:00",
                "priority": 1,
                "type": "%s"
            }
            """, input, classification, classification);
    }

    public ScheduleOptimizationRequest convertToScheduleRequest(String input) {
        try {
            String prompt = createPrompt(input);
            String gptResponse = callGptApi(prompt);
            
            Map<String, Object> scheduleInfo = objectMapper.readValue(gptResponse, Map.class);
            
            // 위치 정보 조회 및 변환
            ScheduleDto schedule = convertToScheduleDto(scheduleInfo);
            
            // 응답 생성
            ScheduleOptimizationRequest request = new ScheduleOptimizationRequest();
            request.setFixedSchedules(Collections.singletonList(schedule));
            request.setFlexibleSchedules(new ArrayList<>());
            
            return request;
        } catch (Exception e) {
            log.error("Failed to convert input to schedule: {}", e.getMessage(), e);
            throw new ScheduleProcessException("Failed to convert schedule", e);
        }
    }

    private String createPrompt(String input) {
        LocalDateTime nextWeekend = LocalDateTime.now()
            .with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
            .withHour(10).withMinute(0);
            
        return String.format("""
            다음 텍스트에서 일정 정보를 추출하여 JSON 형식으로 변환해주세요.
            오늘은 %s이고, 주말은 %s입니다.
            
            입력: %s
            
            다음 형식으로 실제 날짜와 시간을 포함하여 응답해주세요:
            {
                "name": "일정명",
                "date": "%s",
                "time": "10:00",
                "duration": 60,
                "location": "장소명",
                "type": "FLEXIBLE",
                "priority": 2
            }
            """, 
            LocalDate.now().format(DateTimeFormatter.ISO_DATE),
            nextWeekend.format(DateTimeFormatter.ISO_DATE),
            input,
            nextWeekend.format(DateTimeFormatter.ISO_DATE)
        );
    }

    private ScheduleDto convertToScheduleDto(Map<String, Object> info) {
        try {
            // 날짜와 시간 파싱
            String date = (String) info.getOrDefault("date", 
                LocalDate.now().format(DateTimeFormatter.ISO_DATE));
            String time = (String) info.getOrDefault("time", "10:00");
            
            LocalDateTime startTime;
            try {
                startTime = LocalDateTime.parse(date + "T" + time);
            } catch (DateTimeParseException e) {
                startTime = LocalDateTime.now()
                    .with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
                    .withHour(10).withMinute(0);
                log.warn("Failed to parse date/time. Using default: {}", startTime);
            }
            
            // 위치 정보 조회
            String locationName = (String) info.get("location");
            Map<String, Object> coordinates = placeSearchService.searchPlace(locationName);
            
            LocationDto location = new LocationDto();
            location.setLatitude(Double.parseDouble((String) coordinates.get("latitude")));
            location.setLongitude(Double.parseDouble((String) coordinates.get("longitude")));
            
            // 기본 60분으로 설정된 duration 사용
            int durationMinutes = ((Number) info.getOrDefault("duration", 60)).intValue();
            
            ScheduleDto schedule = new ScheduleDto();
            schedule.setName((String) info.get("name"));
            schedule.setStartTime(startTime);
            schedule.setEndTime(startTime.plusMinutes(durationMinutes));
            schedule.setLocation(location);
            schedule.setType((String) info.get("type"));
            schedule.setPriority(((Number) info.getOrDefault("priority", 1)).intValue());
            
            return schedule;
        } catch (Exception e) {
            log.error("Error converting schedule data: {}", info, e);
            throw new ScheduleProcessException("Failed to convert schedule data", e);
        }
    }

    private String callGptApi(String prompt) {
        return retryTemplate.execute(context -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4");
            requestBody.put("messages", Arrays.asList(
                Map.of("role", "system", "content", "일정 관리 시스템입니다."),
                message
            ));
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 1000);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.debug("Calling GPT API with prompt: {}", prompt);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                try {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) 
                        response.getBody().get("choices");
                    
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> choice = choices.get(0);
                        Map<String, Object> messageResponse = (Map<String, Object>) choice.get("message");
                        String content = (String) messageResponse.get("content");
                        
                        log.debug("Received GPT response: {}", content);
                        return content;
                    }
                } catch (Exception e) {
                    log.error("Error parsing GPT response: {}", e.getMessage());
                    throw new ScheduleProcessException("Failed to parse GPT response", e);
                }
            }

            throw new ScheduleProcessException("Invalid response from GPT API");
        });
    }

    private ScheduleData convertToScheduleData(Map<String, Object> details) {
        try {
            return ScheduleData.builder()
                .location(String.valueOf(details.get("location")))
                .time(String.valueOf(details.get("time")))
                .priority(Integer.parseInt(String.valueOf(details.get("priority"))))
                .type(ScheduleType.valueOf(String.valueOf(details.get("type"))))
                .build();
        } catch (Exception e) {
            log.error("Failed to convert schedule data: {}", details, e);
            throw new ScheduleProcessException("Failed to convert schedule data", e);
        }
    }
}