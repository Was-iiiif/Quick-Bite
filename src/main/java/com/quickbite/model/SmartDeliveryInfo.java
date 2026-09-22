package com.quickbite.model;

/**
 * Data Transfer Object (DTO) for Smart Delivery Information
 * obtained asynchronously via external weather API and routing calculation.
 */
public class SmartDeliveryInfo {
    private double temperatureCelsius;
    private String weatherCondition;
    private int estimatedMinutes;
    private String weatherAdvisory;
    private boolean isSimulatedFallback;

    public SmartDeliveryInfo() {
        this.temperatureCelsius = 24.0;
        this.weatherCondition = "Clear Sky";
        this.estimatedMinutes = 25;
        this.weatherAdvisory = "Optimal driving conditions. Expect fast delivery!";
        this.isSimulatedFallback = true;
    }

    public SmartDeliveryInfo(double temperatureCelsius, String weatherCondition, int estimatedMinutes, String weatherAdvisory, boolean isSimulatedFallback) {
        this.temperatureCelsius = temperatureCelsius;
        this.weatherCondition = weatherCondition;
        this.estimatedMinutes = estimatedMinutes;
        this.weatherAdvisory = weatherAdvisory;
        this.isSimulatedFallback = isSimulatedFallback;
    }

    public double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public void setTemperatureCelsius(double temperatureCelsius) {
        this.temperatureCelsius = temperatureCelsius;
    }

    public String getWeatherCondition() {
        return weatherCondition;
    }

    public void setWeatherCondition(String weatherCondition) {
        this.weatherCondition = weatherCondition;
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(int estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public String getWeatherAdvisory() {
        return weatherAdvisory;
    }

    public void setWeatherAdvisory(String weatherAdvisory) {
        this.weatherAdvisory = weatherAdvisory;
    }

    public boolean isSimulatedFallback() {
        return isSimulatedFallback;
    }

    public void setSimulatedFallback(boolean simulatedFallback) {
        isSimulatedFallback = simulatedFallback;
    }

    @Override
    public String toString() {
        return String.format("%s, %.1f°C | ETA: ~%d mins (%s)", weatherCondition, temperatureCelsius, estimatedMinutes, weatherAdvisory);
    }
}
