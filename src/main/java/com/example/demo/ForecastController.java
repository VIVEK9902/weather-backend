package com.example.demo;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api")
public class ForecastController {

    @Value("${weatherapi.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/forecast")
    public List<ForecastDayDTO> getForecast(
            @RequestParam String city,
            @RequestParam(defaultValue = "C") String unit
    ) {

        try {
            String url = String.format(
                    "https://api.weatherapi.com/v1/forecast.json?key=%s&q=%s&days=7",
                    apiKey,
                    city
            );

            WeatherForecastResponse response =
                    restTemplate.getForObject(url, WeatherForecastResponse.class);

            if (response == null || response.getForecast() == null) return List.of();

            return Arrays.stream(response.getForecast().getForecastday())
                    .map(day -> new ForecastDayDTO(
                            day.getDate(),
                            unit.equals("F") ? day.getDay().getMaxtemp_f() : day.getDay().getMaxtemp_c(),
                            unit.equals("F") ? day.getDay().getMintemp_f() : day.getDay().getMintemp_c(),
                            day.getDay().getCondition().getText(),
                            day.getDay().getCondition().getIcon()
                    ))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            return List.of();
        }
    }


    /* =====================================================
       =============== DTOs (keep simple) ==================
       ===================================================== */

    public static class ForecastDayDTO {
        public String date;
        public double max;
        public double min;
        public String condition;
        public String icon;

        public ForecastDayDTO(String date, double max, double min, String condition, String icon) {
            this.date = date;
            this.max = max;
            this.min = min;
            this.condition = condition;
            this.icon = icon;
        }
    }


    /* ========== WeatherAPI mapping classes ========== */

    static class WeatherForecastResponse {
        private Forecast forecast;
        public Forecast getForecast() { return forecast; }
        public void setForecast(Forecast forecast) { this.forecast = forecast; }
    }

    static class Forecast {
        private ForecastDay[] forecastday;
        public ForecastDay[] getForecastday() { return forecastday; }
        public void setForecastday(ForecastDay[] forecastday) { this.forecastday = forecastday; }
    }

    static class ForecastDay {
        private String date;
        private Day day;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public Day getDay() { return day; }
        public void setDay(Day day) { this.day = day; }
    }

    static class Day {
        private double maxtemp_c;
        private double mintemp_c;
        private double maxtemp_f;
        private double mintemp_f;
        private Condition condition;

        public double getMaxtemp_c() { return maxtemp_c; }
        public double getMintemp_c() { return mintemp_c; }
        public double getMaxtemp_f() { return maxtemp_f; }
        public double getMintemp_f() { return mintemp_f; }

        public Condition getCondition() { return condition; }
        public void setCondition(Condition condition) { this.condition = condition; }
    }

    static class Condition {
        private String text;
        private String icon;

        public String getText() { return text; }
        public String getIcon() { return icon; }

        public void setText(String text) { this.text = text; }
        public void setIcon(String icon) { this.icon = icon; }
    }
}
