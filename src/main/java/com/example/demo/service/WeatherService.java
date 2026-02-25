package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WeatherService {

    @Value("${weatherapi.base.url}")
    private String baseUrl;

    @Value("${weatherapi.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public WeatherService(RestTemplateBuilder builder, ObjectMapper objectMapper) {
        this.restTemplate = builder.build();
        this.objectMapper = objectMapper;
    }

    public JsonNode fetchWeather(String qParam) throws Exception {
        String url = String.format(
                "%s/forecast.json?key=%s&q=%s&days=3&aqi=no&alerts=yes",
                baseUrl, apiKey, qParam
        );
        String body = restTemplate.getForObject(url, String.class);
        return objectMapper.readTree(body);
    }
}
