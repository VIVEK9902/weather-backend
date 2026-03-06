package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api")
public class DetailsController {

    @Value("${weatherapi.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();


    /* ==================================
       GET /api/details
       ================================== */
    @GetMapping("/details")
    public DetailsDTO getDetails(@RequestParam String city) {

        try {
            String url =
                    "https://api.weatherapi.com/v1/current.json?key="
                            + apiKey
                            + "&q=" + city;

            WeatherResponse response =
                    restTemplate.getForObject(url, WeatherResponse.class);

            if (response == null) return new DetailsDTO(0.0, 0.0, 0, 0.0, 0.0, 0.0, 0, 0);

            Current c = response.getCurrent();

            // ✅ FIX: include temperature FIRST
            return new DetailsDTO(
                    c.getTemp_c(),          // ← ADDED
                    c.getFeelslike_c(),
                    c.getHumidity(),
                    c.getWind_kph(),
                    c.getPressure_mb(),
                    c.getUv(),
                    c.getVis_km(),
                    c.getCloud()
            );

        } catch (Exception e) {
            return new DetailsDTO(0.0, 0.0, 0, 0.0, 0.0, 0.0, 0, 0);
        }
    }
    
    



    /* ==================================
       DTO
       ================================== */
    public static class DetailsDTO {

        // ✅ ADDED
        public double temp;

        public double feelsLike;
        public int humidity;
        public double wind;
        public double pressure;
        public double uv;
        public double visibility;
        public int cloud;

        public DetailsDTO(
                double temp,        // ← ADDED
                double feelsLike,
                int humidity,
                double wind,
                double pressure,
                double uv,
                double visibility,
                int cloud
        ) {
            this.temp = temp;      // ← ADDED
            this.feelsLike = feelsLike;
            this.humidity = humidity;
            this.wind = wind;
            this.pressure = pressure;
            this.uv = uv;
            this.visibility = visibility;
            this.cloud = cloud;
        }
    }



    /* ==================================
       WeatherAPI mapping classes
       ================================== */

    static class WeatherResponse {
        private Current current;
        public Current getCurrent() { return current; }
    }

    static class Current {

        // ✅ ADDED
        private double temp_c;

        private double feelslike_c;
        private int humidity;
        private double wind_kph;
        private double pressure_mb;
        private double uv;
        private double vis_km;
        private int cloud;

        // ✅ ADDED
        public double getTemp_c() { return temp_c; }

        public double getFeelslike_c() { return feelslike_c; }
        public int getHumidity() { return humidity; }
        public double getWind_kph() { return wind_kph; }
        public double getPressure_mb() { return pressure_mb; }
        public double getUv() { return uv; }
        public double getVis_km() { return vis_km; }
        public int getCloud() { return cloud; }
    }
}
