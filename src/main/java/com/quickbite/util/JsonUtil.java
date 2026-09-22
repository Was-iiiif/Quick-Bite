package com.quickbite.util;

import com.quickbite.model.FoodItem;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for JSON serialization, deserialization, and API response parsing.
 * Features built-in robust JSON formatting and parsing to ensure full autonomy
 * without strict dependency on external third-party JARs, while supporting standard JSON formats.
 */
public class JsonUtil {

    /**
     * Serializes a list of FoodItems into formatted JSON string.
     */
    public static String toJson(List<FoodItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < items.size(); i++) {
            FoodItem item = items.get(i);
            sb.append("  {\n");
            sb.append("    \"id\": ").append(item.getId()).append(",\n");
            sb.append("    \"restaurantId\": ").append(item.getRestaurantId()).append(",\n");
            sb.append("    \"name\": \"").append(escape(item.getName())).append("\",\n");
            sb.append("    \"description\": \"").append(escape(item.getDescription())).append("\",\n");
            sb.append("    \"category\": \"").append(escape(item.getCategory())).append("\",\n");
            sb.append("    \"price\": ").append(item.getPrice()).append(",\n");
            sb.append("    \"available\": ").append(item.isAvailable()).append(",\n");
            sb.append("    \"imageUrl\": \"").append(escape(item.getImageUrl() != null ? item.getImageUrl() : "")).append("\"\n");
            sb.append("  }");
            if (i < items.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Parses a JSON string representing an array of FoodItem objects.
     */
    public static List<FoodItem> parseFoodItemList(String json) {
        List<FoodItem> list = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) {
            return list;
        }

        // Match individual JSON objects {...}
        Pattern objPattern = Pattern.compile("\\{([^{}]+)\\}");
        Matcher objMatcher = objPattern.matcher(json);

        while (objMatcher.find()) {
            String objContent = objMatcher.group(1);
            FoodItem item = new FoodItem();

            // Extract fields
            item.setId(extractIntField(objContent, "id", 0));
            item.setRestaurantId(extractIntField(objContent, "restaurantId", 1));
            item.setName(extractStringField(objContent, "name", "Unnamed Item"));
            item.setDescription(extractStringField(objContent, "description", ""));
            item.setCategory(extractStringField(objContent, "category", "Main"));
            item.setPrice(extractDoubleField(objContent, "price", 0.0));
            item.setAvailable(extractBooleanField(objContent, "available", true));
            item.setImageUrl(extractStringField(objContent, "imageUrl", ""));

            list.add(item);
        }

        return list;
    }

    /**
     * Extracts a string field from a JSON object block.
     */
    public static String extractStringField(String jsonBlock, String fieldName, String defaultValue) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(jsonBlock);
        if (matcher.find()) {
            return unescape(matcher.group(1));
        }
        return defaultValue;
    }

    /**
     * Extracts an integer field from a JSON object block.
     */
    public static int extractIntField(String jsonBlock, String fieldName, int defaultValue) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*(-?\\d+)");
        Matcher matcher = pattern.matcher(jsonBlock);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    /**
     * Extracts a double/floating-point field from a JSON object block.
     */
    public static double extractDoubleField(String jsonBlock, String fieldName, double defaultValue) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*(-?\\d+(\\.\\d+)?)");
        Matcher matcher = pattern.matcher(jsonBlock);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    /**
     * Extracts a boolean field from a JSON object block.
     */
    public static boolean extractBooleanField(String jsonBlock, String fieldName, boolean defaultValue) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*(true|false)");
        Matcher matcher = pattern.matcher(jsonBlock);
        if (matcher.find()) {
            return Boolean.parseBoolean(matcher.group(1));
        }
        return defaultValue;
    }

    /**
     * Saves JSON string to a file.
     */
    public static void saveToFile(String json, File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(json);
        }
    }

    /**
     * Reads entire file contents as string.
     */
    public static String readFromFile(File file) throws IOException {
        return new String(Files.readAllBytes(Paths.get(file.toURI())));
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String unescape(String s) {
        if (s == null) return "";
        return s.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }
}
