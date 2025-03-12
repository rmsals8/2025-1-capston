package com.example.TripSpring.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Configuration
public class RestTemplateConfig {
    
    @Bean(name = "simpleRestTemplate")
    @Primary  // 기본 RestTemplate으로 사용
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(30000);
        
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.getMessageConverters()
            .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        
        return restTemplate;
    }

    @Bean(name = "builderRestTemplate")
    public RestTemplate restTemplateWithBuilder(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(30))
            .build();
    }
}