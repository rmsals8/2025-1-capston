package com.example.TripSpring.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GptResponseParser {
    private final ObjectMapper objectMapper;

    public String parseGptResponse(String responseBody) {
        try {
            Map<String, Object> response = objectMapper.readValue(responseBody, 
                new TypeReference<Map<String, Object>>() {});
            
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> firstChoice = choices.get(0);
                Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                if (message != null) {
                    return (String) message.get("content");
                }
            }
            throw new IllegalStateException("Invalid GPT API response structure");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse GPT API response", e);
        }
    }
}