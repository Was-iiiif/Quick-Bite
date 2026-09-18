package com.quickbite.api;

import com.quickbite.concurrency.ConcurrencyMonitorService;
import com.quickbite.model.SmartDeliveryInfo;
import com.quickbite.util.JsonUtil;
import javafx.application.Platform;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Service that calculates Smart Delivery Information (ETA + Weather Safety Advisory)
 * by integrating live external weather API data and handling network fallbacks.
 */
public class SmartDeliveryService {
    private static final SmartDeliveryService INSTANCE = new SmartDeliveryService();

    private final WeatherApiClient weatherClient = new WeatherApiClient();
    private final ConcurrencyMonitorService monitor = ConcurrencyMonitorService.getInstance();

    private SmartDeliveryService() {}

    public static SmartDeliveryService getInstance() {
        return INSTANCE;
    }

    /**
     * Asynchronously retrieves smart delivery information on a background thread.
     * Guaranteed never to throw exceptions to the UI — falls back to local estimation if network fails.
     *
     * @param onComplete Callback invoked on JavaFX Application Thread with the resulting DTO
     */
    public void fetchSmartDeliveryInfoAsync(Consumer<SmartDeliveryInfo> onComplete) {
        CompletableFuture.supplyAsync(() -> {
            try {
                monitor.logEvent("SmartDelivery", "Querying live weather for delivery area...");
                String json = weatherClient.fetchCurrentWeatherJson();

                // Parse current weather block from JSON
                // Example format: "current":{"temperature_2m":18.4,"weather_code":3,...}
                double temp = 22.0;
                int code = 1;

                int currentIdx = json.indexOf("\"current\"");
                if (currentIdx != -1) {
                    String currentBlock = json.substring(currentIdx);
                    temp = JsonUtil.extractDoubleField(currentBlock, "temperature_2m", 22.0);
                    code = JsonUtil.extractIntField(currentBlock, "weather_code", 1);
                }

                String condition = WeatherApiClient.mapWmoWeatherCode(code);
                int eta = calculateEta(temp, condition);
                String advisory = generateAdvisory(condition, temp, eta);

                monitor.logEvent("SmartDelivery", String.format("API SUCCESS: %s (%.1f°C), ETA: %d mins", condition, temp, eta));
                return new SmartDeliveryInfo(temp, condition, eta, advisory, false);

            } catch (Exception e) {
                monitor.logEvent("SmartDelivery", "API call failed (" + e.getMessage() + "). Applying smart local fallback.");
                return createFallbackInfo();
            }
        }).thenAccept(info -> {
            try {
                Platform.runLater(() -> {
                    if (onComplete != null) {
                        onComplete.accept(info);
                    }
                });
            } catch (IllegalStateException ignored) {
                if (onComplete != null) {
                    onComplete.accept(info);
                }
            }
        });
    }

    private int calculateEta(double temp, String condition) {
        int baseEta = 25; // 25 minutes base
        if (condition.contains("Rain") || condition.contains("Drizzle")) {
            baseEta += 8;
        } else if (condition.contains("Thunderstorm")) {
            baseEta += 15;
        } else if (condition.contains("Snow")) {
            baseEta += 12;
        }
        if (temp < 0 || temp > 35) {
            baseEta += 5; // Extreme temperatures add buffer
        }
        return baseEta;
    }

    private String generateAdvisory(String condition, double temp, int eta) {
        if (condition.contains("Rain") || condition.contains("Drizzle")) {
            return "Wet road conditions. Rider taking careful routes for safety (+8 mins ETA).";
        } else if (condition.contains("Thunderstorm")) {
            return "Severe storm alert. Riders driving cautiously with waterproof insulated bags.";
        } else if (condition.contains("Snow")) {
            return "Snow/ice warning. Deliveries dispatched with tire chains (+12 mins ETA).";
        } else if (temp > 32) {
            return "Hot weather advisory. Cold drinks kept in chilled compartments.";
        } else {
            return "Optimal riding weather. Expected swift delivery directly to your door!";
        }
    }

    private SmartDeliveryInfo createFallbackInfo() {
        return new SmartDeliveryInfo(
                23.5,
                "Clear & Mild (Fallback)",
                25,
                "Live API offline. Calculated standard delivery duration for your local area (~25 mins).",
                true
        );
    }
}
