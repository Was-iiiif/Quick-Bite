package com.quickbite.service;

import com.quickbite.dao.FoodItemDAO;
import com.quickbite.model.FoodItem;
import com.quickbite.util.JsonUtil;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Service managing food items, menu CRUD operations, and JSON import/export.
 */
public class MenuService {
    private final FoodItemDAO foodItemDAO = new FoodItemDAO();

    public List<FoodItem> getFoodItems(int restaurantId) {
        return foodItemDAO.getByRestaurantId(restaurantId);
    }

    public boolean addFoodItem(FoodItem item) {
        if (item.getName() == null || item.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be empty.");
        }
        if (item.getPrice() < 0) {
            throw new IllegalArgumentException("Item price must be positive.");
        }
        return foodItemDAO.create(item);
    }

    public boolean updateFoodItem(FoodItem item) {
        return foodItemDAO.update(item);
    }

    public boolean deleteFoodItem(int id) {
        return foodItemDAO.delete(id);
    }

    public boolean toggleAvailability(int id, boolean available) {
        return foodItemDAO.toggleAvailability(id, available);
    }

    public List<String> getCategories(int restaurantId) {
        return foodItemDAO.getCategories(restaurantId);
    }

    /**
     * Exports a restaurant's menu to a JSON file.
     */
    public void exportMenuToJson(int restaurantId, File file) throws IOException {
        List<FoodItem> items = foodItemDAO.getByRestaurantId(restaurantId);
        String json = JsonUtil.toJson(items);
        JsonUtil.saveToFile(json, file);
    }

    /**
     * Imports food items from a JSON file into SQLite.
     *
     * @return Number of imported items
     */
    public int importMenuFromJson(int restaurantId, File file) throws IOException {
        String json = JsonUtil.readFromFile(file);
        List<FoodItem> importedItems = JsonUtil.parseFoodItemList(json);
        int successCount = 0;
        for (FoodItem item : importedItems) {
            item.setRestaurantId(restaurantId);
            if (foodItemDAO.create(item)) {
                successCount++;
            }
        }
        return successCount;
    }
}
