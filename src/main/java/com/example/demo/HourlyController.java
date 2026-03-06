package com.example.demo;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api")
public class HourlyController {

    @Value("${weatherapi.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/hourly")
    public List<HourlyDTO> getHourly(@RequestParam String city) {

        try {
            String url = String.format(
                "https://api.weatherapi.com/v1/forecast.json?key=%s&q=%s&days=1",
                apiKey,
                city
            );

            WeatherHourlyResponse response =
                    restTemplate.getForObject(url, WeatherHourlyResponse.class);

            if (response == null) return List.of();

            return Arrays.stream(response.getForecast()
                    .getForecastday()[0]
                    .getHour())
                    .limit(24)
                    .map(h -> new HourlyDTO(
                    		h.getTime().substring(11, 16),
                            h.getTemp_c(),
                            h.getFeelslike_c(),
                            h.getHumidity(),
                            h.getWind_kph(),
                            h.getChance_of_rain(),
                            h.getCondition().getText(),
                            h.getCondition().getIcon()
                    ))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            return List.of();
        }
    }


    /* ================= DTOs ================= */

    public static class HourlyDTO {
        public String time;
        public double temp;
        public double feelsLike;
        public int humidity;
        public double wind;
        public double rain;
        public String condition;
        public String icon;

        public HourlyDTO(String time, double temp, double feelsLike,
                         int humidity, double wind, double rain,
                         String condition, String icon) {
            this.time = time;
            this.temp = temp;
            this.feelsLike = feelsLike;
            this.humidity = humidity;
            this.wind = wind;
            this.rain = rain;
            this.condition = condition;
            this.icon = icon;
        }
    }

    /* ===== WeatherAPI mapping ===== */

    static class WeatherHourlyResponse {
        private Forecast forecast;
        public Forecast getForecast() { return forecast; }
        public void setForecast(Forecast forecast) { this.forecast = forecast; }
    }

    static class Forecast {
        private ForecastDay[] forecastday;
        public ForecastDay[] getForecastday() { return forecastday; }
    }

    static class ForecastDay {
        private Hour[] hour;
        public Hour[] getHour() { return hour; }
    }

    static class Hour {

        private String time;
        private double temp_c;
        private double feelslike_c;
        private int humidity;
        private double wind_kph;
        private double chance_of_rain;
        private Condition condition;

        public String getTime() { return time; }
        public double getTemp_c() { return temp_c; }
        public double getFeelslike_c() { return feelslike_c; }
        public int getHumidity() { return humidity; }
        public double getWind_kph() { return wind_kph; }
        public double getChance_of_rain() { return chance_of_rain; }
        public Condition getCondition() { return condition; }
    }

    static class Condition {
        private String text;
        private String icon;

        public String getText() { return text; }
        public String getIcon() { return icon; }
    }
}
