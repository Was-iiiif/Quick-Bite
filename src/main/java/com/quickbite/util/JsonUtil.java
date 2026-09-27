package com.quickbite.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.quickbite.model.FoodItem;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Utility class for JSON serialization and deserialization using Jackson.
 *
 * Provides backward-compatible methods used throughout the QuickBite project.
 */
public final class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private JsonUtil() {
        // Utility class - prevent instantiation
    }

    // ============================================================
    // JSON SERIALIZATION
    // ============================================================

    /**
     * Converts any Java object into a JSON string.
     */
    public static String toJson(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "Failed to serialize object to JSON", e
            );
        }
    }

    /**
     * Converts a list of FoodItem objects into JSON.
     */
    public static String toJson(List<FoodItem> items) {
        return toJson((Object) items);
    }

    // ============================================================
    // JSON DESERIALIZATION
    // ============================================================

    /**
     * Parses a JSON string into a List of FoodItem objects.
     */
    public static List<FoodItem> parseFoodItemList(String json) {
        try {
            return OBJECT_MAPPER.readValue(
                    json,
                    new TypeReference<List<FoodItem>>() {}
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "Failed to parse FoodItem JSON", e
            );
        }
    }

    /**
     * Parses JSON into a Jackson JsonNode.
     *
     * Useful when working with external API responses.
     */
    public static JsonNode parse(String json)
            throws JsonProcessingException {

        return OBJECT_MAPPER.readTree(json);
    }

    // ============================================================
    // JSON FIELD EXTRACTION
    // ============================================================

    /**
     * Extracts a double field from a JSON object.
     */
    public static double extractDoubleField(
            String json,
            String fieldName,
            double defaultValue
    ) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            JsonNode value = root.get(fieldName);

            if (value != null && value.isNumber()) {
                return value.asDouble();
            }

            return defaultValue;

        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Extracts an integer field from a JSON object.
     */
    public static int extractIntField(
            String json,
            String fieldName,
            int defaultValue
    ) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            JsonNode value = root.get(fieldName);

            if (value != null && value.isNumber()) {
                return value.asInt();
            }

            return defaultValue;

        } catch (Exception e) {
            return defaultValue;
        }
    }

    // ============================================================
    // FILE OPERATIONS
    // ============================================================

    /**
     * Saves a JSON string to a file.
     */
    public static void saveToFile(
            String json,
            File file
    ) throws IOException {

        JsonNode jsonNode = OBJECT_MAPPER.readTree(json);
        OBJECT_MAPPER.writeValue(file, jsonNode);
    }

    /**
     * Writes a list of FoodItem objects directly to a JSON file.
     *
     * Kept for compatibility with existing QuickBite code.
     */
    public static void writeToFile(
            List<FoodItem> items,
            File file
    ) throws IOException {

        OBJECT_MAPPER.writeValue(file, items);
    }

    /**
     * Reads raw JSON content from a file.
     */
    public static String readFromFile(
            File file
    ) throws IOException {

        return OBJECT_MAPPER.readTree(file).toString();
    }

    /**
     * Reads a JSON file into any requested Java type.
     *
     * This overload fixes the current compilation error:
     *
     * JsonUtil.readFromFile(
     *     file,
     *     new TypeReference<List<FoodItem>>() {}
     * );
     */
    public static <T> T readFromFile(
            File file,
            TypeReference<T> typeReference
    ) throws IOException {

        return OBJECT_MAPPER.readValue(file, typeReference);
    }

    /**
     * Reads FoodItem objects directly from a JSON file.
     */
    public static List<FoodItem> readFoodItemsFromFile(
            File file
    ) throws IOException {

        return OBJECT_MAPPER.readValue(
                file,
                new TypeReference<List<FoodItem>>() {}
        );
    }
}