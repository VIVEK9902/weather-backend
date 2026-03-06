package com.example.demo;

import com.example.demo.service.WeatherService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * WeatherController:
 * - existing endpoint: GET /api/weather   (unchanged)
 * - NEW endpoint:      GET /api/weather/forecast  (returns 7-day daily forecast via WeatherAPI)
 */
@RestController
@RequestMapping("/api/weather")
@CrossOrigin(origins = "${cors.allowed.origin}")
public class WeatherController {

    private final WeatherService weatherService;

    // WeatherAPI config (set in application.properties)
    @Value("${weatherapi.api.key:}")
    private String weatherApiKey;

    @Value("${weatherapi.base.url:http://api.weatherapi.com/v1}")
    private String weatherApiBaseUrl;

    private final RestTemplate rest = new RestTemplate();

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    /**
     * Existing endpoint - unchanged
     * Accepts:
     *  - ?city=CityName
     *  - or ?lat=12.34&lon=56.78
     *
     * Returns JSON with current weather and a forecast array (3 days).
     */
    @GetMapping
    public ResponseEntity<?> getWeather(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon) {

        try {
            // Decide query parameter
            String qParam;
            if (city != null && !city.isBlank()) {
                qParam = city.trim();
            } else if (lat != null && lon != null) {
                qParam = String.format("%f,%f", lat, lon);
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Missing parameters",
                        "message", "Please provide either city or lat & lon"
                ));
            }

            // Call service (your existing WeatherService)
            JsonNode root = weatherService.fetchWeather(qParam);

            // Prepare response
            Map<String, Object> out = new HashMap<>();

            // Location
            JsonNode location = root.path("location");
            out.put("city", location.path("name").asText(""));
            out.put("region", location.path("region").asText(""));
            out.put("country", location.path("country").asText(""));
            
            out.put("lat", location.path("lat").asDouble(0.0));
            out.put("lon", location.path("lon").asDouble(0.0));

            // Current
            JsonNode current = root.path("current");
            out.put("temp_c", current.path("temp_c").asDouble(Double.NaN));
            out.put("feelslike_c", current.path("feelslike_c").asDouble(Double.NaN));
            out.put("humidity", current.path("humidity").asInt(-1));
            out.put("pressure_mb", current.path("pressure_mb").asInt(-1));
            out.put("wind_kph", current.path("wind_kph").asDouble(Double.NaN));
            out.put("wind_dir", current.path("wind_dir").asText(""));
            out.put("vis_km", current.path("vis_km").asDouble(Double.NaN));
            out.put("uv", current.path("uv").asDouble(Double.NaN));
            out.put("temp_f", current.path("temp_f").asDouble(Double.NaN));
            out.put("feelslike_f", current.path("feelslike_f").asDouble(Double.NaN));


            String conditionText = current.path("condition").path("text").asText("");
            out.put("condition", conditionText);

            String rawIcon = current.path("condition").path("icon").asText("");
            if (rawIcon.startsWith("//")) rawIcon = "https:" + rawIcon;
            out.put("icon", rawIcon);

            // Forecast (3 days) - same as before
            List<Map<String, Object>> forecastList = new ArrayList<>();
            JsonNode forecastDays = root.path("forecast").path("forecastday");
            if (forecastDays.isArray()) {
                for (JsonNode dayNode : forecastDays) {
                    Map<String, Object> day = new HashMap<>();
                    day.put("date", dayNode.path("date").asText());

                    JsonNode dayInfo = dayNode.path("day");
                    day.put("avg_temp_c", dayInfo.path("avgtemp_c").asDouble(Double.NaN));
                    day.put("max_temp_c", dayInfo.path("maxtemp_c").asDouble(Double.NaN));
                    day.put("min_temp_c", dayInfo.path("mintemp_c").asDouble(Double.NaN));
                    day.put("avg_temp_f", dayInfo.path("avgtemp_f").asDouble(Double.NaN));
                    day.put("max_temp_f", dayInfo.path("maxtemp_f").asDouble(Double.NaN));
                    day.put("min_temp_f", dayInfo.path("mintemp_f").asDouble(Double.NaN));


                    String dcond = dayInfo.path("condition").path("text").asText("");
                    day.put("condition", dcond);

                    String dicon = dayInfo.path("condition").path("icon").asText("");
                    if (dicon.startsWith("//")) dicon = "https:" + dicon;
                    day.put("icon", dicon);

                    forecastList.add(day);
                }
            }
            out.put("forecast", forecastList);

            return ResponseEntity.ok(out);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to fetch weather",
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * NEW endpoint: GET /api/weather/forecast
     * Uses WeatherAPI (weatherapi.com) forecast.json to return 7-day daily forecast.
     * Accepts: ?city=CityName  OR  ?lat=...&lon=...
     * Optional: ?units=metric|imperial  (default metric)
     */
    
    
    @GetMapping("/forecast")
    public ResponseEntity<?> get7DayForecast(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false, defaultValue = "metric") String units
    ) {
        try {
            // Validate input
            if ((lat == null || lon == null) && (city == null || city.isBlank())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Provide city or lat+lon"));
            }

            if (weatherApiKey == null || weatherApiKey.isBlank()) {
                return ResponseEntity.status(500).body(Map.of("error", "Server missing weatherapi.api.key property"));
            }

            // Build query q parameter (WeatherAPI accepts "lat,lon" or city)
            String q;
            if (city != null && !city.isBlank()) {
                q = URLEncoder.encode(city.trim(), StandardCharsets.UTF_8);
            } else {
                q = String.format("%s,%s", lat, lon);
            }

            // days=7 -> WeatherAPI returns forecastday[] (usually 7)
            String url = String.format("%s/forecast.json?key=%s&q=%s&days=7&aqi=no&alerts=no",
                    weatherApiBaseUrl.replaceAll("/$", ""), weatherApiKey, q);

            // Fetch
            Map<?,?> resp = rest.getForObject(URI.create(url), Map.class);
            if (resp == null) {
                return ResponseEntity.status(502).body(Map.of("error", "No response from weather provider"));
            }

            // Extract lat/lon/timezone if present
            Map<String, Object> out = new HashMap<>();
            Object locationObj = resp.get("location");
            if (locationObj instanceof Map) {
                Map<?,?> loc = (Map<?,?>) locationObj;
                out.put("lat", loc.containsKey("lat") ? loc.get("lat") : null);
                out.put("lon", loc.containsKey("lon") ? loc.get("lon") : null);
                out.put("timezone", loc.containsKey("tz_id") ? loc.get("tz_id") : resp.get("timezone"));

            } else {
                out.put("lat", lat);
                out.put("lon", lon);
            }

            // Extract forecastday list
            Object forecastObj = resp.get("forecast");
            List<?> forecastDays = Collections.emptyList();
            if (forecastObj instanceof Map) {
                Object fd = ((Map<?,?>) forecastObj).get("forecastday");
                if (fd instanceof List) forecastDays = (List<?>) fd;
            }

            List<Map<String, Object>> daysOut = new ArrayList<>();
            for (Object o : forecastDays) {
                if (!(o instanceof Map)) continue;
                Map<?,?> dayMap = (Map<?,?>) o;
                Map<String, Object> dayOut = new HashMap<>();

                // date_epoch available in WeatherAPI (seconds)
                Object dateEpoch = dayMap.get("date_epoch");
                if (dateEpoch instanceof Number) {
                    dayOut.put("dt", ((Number) dateEpoch).longValue());
                } else {
                    // fallback: keep date string and attempt no conversion
                    dayOut.put("dt", dayMap.get("date"));
                }

                // day object
                Object dayObj = dayMap.get("day");
                if (dayObj instanceof Map) {
                    Map<?,?> d = (Map<?,?>) dayObj;
                    // pick metric or imperial fields
                    if ("imperial".equalsIgnoreCase(units)) {
                        // WeatherAPI provides *_f and maxwind_mph
                        Object avgF = d.get("avgtemp_f");
                        Object minF = d.get("mintemp_f");
                        Object maxF = d.get("maxtemp_f");
                        dayOut.put("temp_day", avgF);
                        dayOut.put("temp_min", minF);
                        dayOut.put("temp_max", maxF);
                        // wind in mph
                        dayOut.put("wind_speed", d.containsKey("maxwind_mph") ? d.get("maxwind_mph") : d.get("maxwind_kph"));

                    } else {
                        // metric
                        Object avgC = d.get("avgtemp_c");
                        Object minC = d.get("mintemp_c");
                        Object maxC = d.get("maxtemp_c");
                        dayOut.put("temp_day", avgC);
                        dayOut.put("temp_min", minC);
                        dayOut.put("temp_max", maxC);
                        // wind in kph
                        dayOut.put("wind_speed", d.containsKey("maxwind_kph") ? d.get("maxwind_kph") : d.get("maxwind_mph"));

                    }
                    // humidity
                    dayOut.put("humidity", d.get("avghumidity"));
                    // condition
                    Object condObj = d.get("condition");
                    if (condObj instanceof Map) {
                        Map<?,?> cond = (Map<?,?>) condObj;
                        dayOut.put("weather_desc", cond.get("text"));
                        String icon = Objects.toString(cond.get("icon"), "");
                        if (icon.startsWith("//")) icon = "https:" + icon;
                        dayOut.put("weather_icon", icon);
                    }
                }
                daysOut.add(dayOut);
            }

            out.put("daily", daysOut);
            return ResponseEntity.ok(out);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Forecast fetch failed", "detail", e.getMessage()));
        }
    }
    
    @GetMapping("/cities")
    public ResponseEntity<?> searchCities(@RequestParam String q) {
        try {
            String url = String.format(
                    "%s/search.json?key=%s&q=%s",
                    weatherApiBaseUrl.replaceAll("/$", ""),
                    weatherApiKey,
                    URLEncoder.encode(q, StandardCharsets.UTF_8)
            );

            List<?> resp = rest.getForObject(URI.create(url), List.class);

            List<String> names = new ArrayList<>();
            if (resp != null) {
                for (Object o : resp) {
                    if (o instanceof Map) {
                        Map<?, ?> m = (Map<?, ?>) o;
                        names.add(
                                m.get("name") + ", " + m.get("country")
                        );
                    }
                }
            }

            return ResponseEntity.ok(names);

        } catch (Exception e) {
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

}
