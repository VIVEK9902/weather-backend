package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api")
public class MonthlyController {

    @Value("${weatherapi.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();


    /* ===============================
       GET /api/monthly
       =============================== */
    @GetMapping("/monthly")
    public List<MonthlyDTO> getMonthly(
            @RequestParam String city
    ) {

        try {
            String url =
                    "https://api.weatherapi.com/v1/forecast.json?key="
                            + apiKey
                            + "&q=" + city
                            + "&days=7"; // free plan max


            WeatherForecastResponse response =
                    restTemplate.getForObject(url, WeatherForecastResponse.class);

            if (response == null) return List.of();

            return Arrays.stream(response.getForecast().getForecastday())
                    .map(d -> new MonthlyDTO(
                            d.getDate(),
                            d.getDay().getMaxtemp_c(),
                            d.getDay().getMintemp_c(),
                            d.getDay().getCondition().getText(),
                            d.getDay().getCondition().getIcon()
                    ))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            return List.of();
        }
    }



    /* ===============================
       DTO
       =============================== */
    public static class MonthlyDTO {
        public String date;
        public double max;
        public double min;
        public String condition;
        public String icon;

        public MonthlyDTO(String date, double max, double min,
                          String condition, String icon) {
            this.date = date;
            this.max = max;
            this.min = min;
            this.condition = condition;
            this.icon = icon;
        }
    }



    /* ===============================
       WeatherAPI Mapping Classes
       =============================== */

    static class WeatherForecastResponse {
        private Forecast forecast;

        public Forecast getForecast() { return forecast; }
    }

    static class Forecast {
        private ForecastDay[] forecastday;

        public ForecastDay[] getForecastday() { return forecastday; }
    }

    static class ForecastDay {
        private String date;
        private Day day;

        public String getDate() { return date; }
        public Day getDay() { return day; }
    }

    static class Day {
        private double maxtemp_c;
        private double mintemp_c;
        private Condition condition;

        public double getMaxtemp_c() { return maxtemp_c; }
        public double getMintemp_c() { return mintemp_c; }
        public Condition getCondition() { return condition; }
    }

    static class Condition {
        private String text;
        private String icon;

        public String getText() { return text; }
        public String getIcon() { return icon; }
    }
}
