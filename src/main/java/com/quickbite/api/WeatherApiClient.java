package com.quickbite.api;

import com.quickbite.concurrency.ConcurrencyMonitorService;
import com.quickbite.util.JsonUtil;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Client for fetching real-time weather data using Java 11+ HttpClient.
 * Uses public Open-Meteo REST API (free, open, no API key required).
 */
public class WeatherApiClient {
    private static final String API_URL = "https://api.open-meteo.com/v1/forecast?latitude=40.7128&longitude=-74.0060&current=temperature_2m,weather_code,wind_speed_10m";

    private final HttpClient httpClient;
    private final ConcurrencyMonitorService monitor = ConcurrencyMonitorService.getInstance();

    public WeatherApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    /**
     * Performs an HTTP GET request to the weather API.
     * Must be called from a background thread or async executor.
     *
     * @return Raw JSON response string
     * @throws Exception if network is unreachable, timeout occurs, or HTTP code is not 200
     */
    public String fetchCurrentWeatherJson() throws Exception {
        monitor.logEvent("WeatherAPI", "Sending asynchronous HTTP GET request to " + API_URL);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(4))
                .header("Accept", "application/json")
                .header("User-Agent", "QuickBite-JavaFX-Client/1.0")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            monitor.logEvent("WeatherAPI", "Received HTTP 200 OK response from Weather API (" + response.body().length() + " bytes)");
            return response.body();
        } else {
            throw new RuntimeException("HTTP GET failed with status: " + response.statusCode());
        }
    }

    /**
     * Translates WMO weather codes into human-readable descriptions.
     */
    public static String mapWmoWeatherCode(int code) {
        if (code == 0) return "Clear Sky";
        if (code >= 1 && code <= 3) return "Partly Cloudy";
        if (code == 45 || code == 48) return "Foggy";
        if (code >= 51 && code <= 55) return "Light Drizzle";
        if (code >= 61 && code <= 65) return "Rainy";
        if (code >= 71 && code <= 77) return "Snow Flurries";
        if (code >= 80 && code <= 82) return "Heavy Rain Showers";
        if (code >= 95 && code <= 99) return "Thunderstorm";
        return "Mild Overcast";
    }
}
