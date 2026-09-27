package com.quickbite.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.quickbite.concurrency.ConcurrencyMonitorService;
import com.quickbite.model.SmartDeliveryInfo;
import com.quickbite.util.JsonUtil;
import javafx.application.Platform;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Service that calculates Smart Delivery Information
 * (ETA + Weather Safety Advisory)
 * by integrating live external weather API data.
 *
 * JSON parsing is handled using Jackson.
 */
public class SmartDeliveryService {

    private static final SmartDeliveryService INSTANCE =
            new SmartDeliveryService();

    private final WeatherApiClient weatherClient =
            new WeatherApiClient();

    private final ConcurrencyMonitorService monitor =
            ConcurrencyMonitorService.getInstance();

    private SmartDeliveryService() {
    }

    public static SmartDeliveryService getInstance() {
        return INSTANCE;
    }

    /**
     * Asynchronously retrieves smart delivery information
     * on a background thread.
     *
     * The callback is invoked on the JavaFX Application Thread.
     *
     * @param onComplete callback receiving SmartDeliveryInfo
     */
    public void fetchSmartDeliveryInfoAsync(
            Consumer<SmartDeliveryInfo> onComplete
    ) {

        CompletableFuture
                .supplyAsync(() -> {

                    try {

                        monitor.logEvent(
                                "SmartDelivery",
                                "Querying live weather for delivery area..."
                        );

                        String json =
                                weatherClient.fetchCurrentWeatherJson();

                        /*
                         * Parse the API response using Jackson.
                         *
                         * Expected Open-Meteo structure:
                         *
                         * {
                         *   "current": {
                         *      "temperature_2m": 28.4,
                         *      "weather_code": 3,
                         *      ...
                         *   }
                         * }
                         */

                        JsonNode root =
                                JsonUtil.parse(json);

                        JsonNode current =
                                root.path("current");

                        double temp =
                                current
                                        .path("temperature_2m")
                                        .asDouble(22.0);

                        int code =
                                current
                                        .path("weather_code")
                                        .asInt(1);

                        String condition =
                                WeatherApiClient
                                        .mapWmoWeatherCode(code);

                        int eta =
                                calculateEta(
                                        temp,
                                        condition
                                );

                        String advisory =
                                generateAdvisory(
                                        condition,
                                        temp,
                                        eta
                                );

                        monitor.logEvent(
                                "SmartDelivery",
                                String.format(
                                        "API SUCCESS: %s (%.1f°C), ETA: %d mins",
                                        condition,
                                        temp,
                                        eta
                                )
                        );

                        return new SmartDeliveryInfo(
                                temp,
                                condition,
                                eta,
                                advisory,
                                false
                        );

                    } catch (Exception e) {

                        monitor.logEvent(
                                "SmartDelivery",
                                "API call failed ("
                                        + e.getMessage()
                                        + "). Applying smart local fallback."
                        );

                        return createFallbackInfo();
                    }

                })
                .thenAccept(info -> {

                    try {

                        Platform.runLater(() -> {

                            if (onComplete != null) {
                                onComplete.accept(info);
                            }

                        });

                    } catch (IllegalStateException ignored) {

                        /*
                         * Handles situations where JavaFX
                         * Platform is no longer available.
                         */
                        if (onComplete != null) {
                            onComplete.accept(info);
                        }
                    }
                });
    }

    /**
     * Calculates estimated delivery time based on weather.
     */
    private int calculateEta(
            double temp,
            String condition
    ) {

        int baseEta = 25;

        if (condition.contains("Rain")
                || condition.contains("Drizzle")) {

            baseEta += 8;

        } else if (condition.contains("Thunderstorm")) {

            baseEta += 15;

        } else if (condition.contains("Snow")) {

            baseEta += 12;
        }

        if (temp < 0 || temp > 35) {
            baseEta += 5;
        }

        return baseEta;
    }

    /**
     * Generates a weather advisory for the customer.
     */
    private String generateAdvisory(
            String condition,
            double temp,
            int eta
    ) {

        if (condition.contains("Rain")
                || condition.contains("Drizzle")) {

            return "Wet road conditions. Rider taking careful "
                    + "routes for safety (+8 mins ETA).";

        } else if (condition.contains("Thunderstorm")) {

            return "Severe storm alert. Riders driving cautiously "
                    + "with waterproof insulated bags.";

        } else if (condition.contains("Snow")) {

            return "Snow/ice warning. Deliveries dispatched "
                    + "with tire chains (+12 mins ETA).";

        } else if (temp > 32) {

            return "Hot weather advisory. Cold drinks kept "
                    + "in chilled compartments.";

        } else {

            return "Optimal riding weather. Expected swift "
                    + "delivery directly to your door!";
        }
    }

    /**
     * Local fallback information when the external API fails.
     */
    private SmartDeliveryInfo createFallbackInfo() {

        return new SmartDeliveryInfo(
                23.5,
                "Clear & Mild (Fallback)",
                25,
                "Live API offline. Calculated standard "
                        + "delivery duration for your local area (~25 mins).",
                true
        );
    }
}