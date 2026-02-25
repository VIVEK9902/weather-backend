package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api")
public class MapController {

    @Value("${weatherapi.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();


    /* =================================
       GET /api/map
       ================================= */
    @GetMapping("/map")
    public MapDTO getMapData(@RequestParam String city) {

        try {

            String url =
                    "https://api.weatherapi.com/v1/current.json?key="
                            + apiKey
                            + "&q=" + city;

            WeatherResponse response =
                    restTemplate.getForObject(url, WeatherResponse.class);

            if (response == null) return null;

            return new MapDTO(
                    response.getLocation().getName(),
                    response.getLocation().getLat(),
                    response.getLocation().getLon(),
                    response.getCurrent().getTemp_c(),
                    response.getCurrent().getCondition().getText(),
                    response.getCurrent().getCondition().getIcon()
            );

        } catch (Exception e) {
            return null;
        }
    }



    /* =================================
       DTO
       ================================= */
    public static class MapDTO {
        public String city;
        public double lat;
        public double lon;
        public double temp;
        public String condition;
        public String icon;

        public MapDTO(String city, double lat, double lon,
                      double temp, String condition, String icon) {
            this.city = city;
            this.lat = lat;
            this.lon = lon;
            this.temp = temp;
            this.condition = condition;
            this.icon = icon;
        }
    }



    /* =================================
       WeatherAPI mapping classes
       ================================= */

    static class WeatherResponse {
        private Location location;
        private Current current;

        public Location getLocation() { return location; }
        public Current getCurrent() { return current; }
    }

    static class Location {
        private String name;
        private double lat;
        private double lon;

        public String getName() { return name; }
        public double getLat() { return lat; }
        public double getLon() { return lon; }
    }

    static class Current {
        private double temp_c;
        private Condition condition;

        public double getTemp_c() { return temp_c; }
        public Condition getCondition() { return condition; }
    }

    static class Condition {
        private String text;
        private String icon;

        public String getText() { return text; }
        public String getIcon() { return icon; }
    }
}
