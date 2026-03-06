package com.example.demo;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api")
public class CitiesController {

    // 🔹 put your key in application.properties
    @Value("${weatherapi.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/cities")
    public List<String> searchCities(@RequestParam String q) {

        try {
            String url = String.format(
                "https://api.weatherapi.com/v1/search.json?key=%s&q=%s",
                apiKey,
                q
            );

            WeatherCity[] response =
                restTemplate.getForObject(url, WeatherCity[].class);

            if (response == null) return List.of();

            // Format: "Mumbai, India"
            return Arrays.stream(response)
                    .map(c -> c.getName() + ", " + c.getCountry())
                    .limit(5)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            // fail safe → return empty list (prevents frontend crash)
            return List.of();
        }
    }

    // 🔹 DTO for WeatherAPI response
    static class WeatherCity {
        private String name;
        private String country;

        public String getName() { return name; }
        public String getCountry() { return country; }

        public void setName(String name) { this.name = name; }
        public void setCountry(String country) { this.country = country; }
    }
}
