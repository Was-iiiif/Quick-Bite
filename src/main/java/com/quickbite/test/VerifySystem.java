package com.quickbite.test;

import com.quickbite.config.DatabaseConfig;
import com.quickbite.dao.*;
import com.quickbite.model.*;
import com.quickbite.service.*;
import com.quickbite.util.JsonUtil;

import java.util.List;
import java.util.Map;

/**
 * Headless verification test to validate database initialization,
 * seeding, DAO transactions, and services without opening a GUI window.
 */
public class VerifySystem {
    public static void main(String[] args) {
        System.out.println("=== QuickBite System Self-Verification ===");

        // 1. Initialize Database & Seed
        DatabaseConfig.initializeDatabase();

        // 2. Test DAOs
        RestaurantDAO restaurantDAO = new RestaurantDAO();
        List<Restaurant> restaurants = restaurantDAO.getAll();
        System.out.println("Restaurants loaded: " + restaurants.size());
        assert !restaurants.isEmpty() : "Restaurants list should not be empty";

        FoodItemDAO foodItemDAO = new FoodItemDAO();
        List<FoodItem> items = foodItemDAO.getByRestaurantId(restaurants.get(0).getId());
        System.out.println("Dishes loaded for " + restaurants.get(0).getName() + ": " + items.size());
        assert !items.isEmpty() : "Food items list should not be empty";

        UserDAO userDAO = new UserDAO();
        User customer = userDAO.authenticate("customer@quickbite.com", "pass123");
        System.out.println("Authenticated customer: " + (customer != null ? customer.getName() : "FAILED"));
        assert customer != null : "Customer authentication failed";

        User admin = userDAO.authenticate("admin@quickbite.com", "pass123");
        System.out.println("Authenticated admin: " + (admin != null ? admin.getName() : "FAILED"));
        assert admin != null : "Admin authentication failed";

        User driver = userDAO.authenticate("driver1@quickbite.com", "pass123");
        System.out.println("Authenticated driver: " + (driver != null ? driver.getName() : "FAILED"));
        assert driver != null : "Driver authentication failed";

        // 3. Test Order Creation Transaction
        OrderService orderService = new OrderService();
        List<CartItem> cart = List.of(new CartItem(items.get(0), 2));
        Order order = orderService.placeOrder(customer.getId(), restaurants.get(0).getId(), cart, "123 Test St", "Cash on Delivery", 3.99, false);
        System.out.println("Placed Test Order #" + order.getId() + " - Total: BDT " + order.getTotalAmount());
        assert order.getId() > 0 : "Order ID should be generated";

        // 4. Test Restaurant Stats
        Map<String, Object> stats = orderService.getRestaurantStatistics(restaurants.get(0).getId());
        System.out.println("Restaurant stats: " + stats);

        // 5. Test JSON Utility
        String json = JsonUtil.toJson(items);
        System.out.println("Serialized JSON menu preview (first 100 chars): " + json.substring(0, Math.min(100, json.length())) + "...");
        List<FoodItem> parsed = JsonUtil.parseFoodItemList(json);
        System.out.println("Parsed items count from JSON: " + parsed.size());
        assert parsed.size() == items.size() : "JSON parsing count mismatch";

        System.out.println("=== ALL VERIFICATION TESTS PASSED SUCCESSFULLY! ===");
    }
}
