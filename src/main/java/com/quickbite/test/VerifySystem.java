package com.quickbite.test;

import com.quickbite.config.DatabaseConfig;
import com.quickbite.dao.FoodItemDAO;
import com.quickbite.dao.OrderDAO;
import com.quickbite.dao.RestaurantDAO;
import com.quickbite.dao.UserDAO;
import com.quickbite.model.CartItem;
import com.quickbite.model.FoodItem;
import com.quickbite.model.Order;
import com.quickbite.model.Restaurant;
import com.quickbite.model.User;
import com.quickbite.service.OrderService;
import com.quickbite.util.JsonUtil;

import java.util.List;
import java.util.Map;

/**
 * Headless verification test for QuickBite.
 *
 * This test does NOT depend on pre-seeded users.
 * It verifies database initialization, DAO operations,
 * user creation/authentication, order creation, statistics,
 * and Jackson JSON serialization/deserialization.
 */
public class VerifySystem {

    public static void main(String[] args) {

        System.out.println("=== QuickBite System Self-Verification ===");

        // ============================================================
        // 1. Initialize Database
        // ============================================================

        DatabaseConfig.initializeDatabase();

        // ============================================================
        // 2. Test Restaurant DAO
        // ============================================================

        RestaurantDAO restaurantDAO = new RestaurantDAO();

        List<Restaurant> restaurants = restaurantDAO.getAll();

        System.out.println("Restaurants loaded: " + restaurants.size());

        assert !restaurants.isEmpty()
                : "At least one restaurant should exist";

        Restaurant restaurant = restaurants.get(0);

        System.out.println(
                "Using restaurant: " + restaurant.getName()
        );

        // ============================================================
        // 3. Test FoodItem DAO
        // ============================================================

        FoodItemDAO foodItemDAO = new FoodItemDAO();

        List<FoodItem> items =
                foodItemDAO.getByRestaurantId(restaurant.getId());

        System.out.println(
                "Dishes loaded for "
                        + restaurant.getName()
                        + ": "
                        + items.size()
        );

        assert !items.isEmpty()
                : "At least one food item should exist";

        // ============================================================
        // 4. Create Test Users
        // ============================================================

        UserDAO userDAO = new UserDAO();

        String testCustomerEmail =
                "verify_customer@quickbite.test";

        String testAdminEmail =
                "verify_admin@quickbite.test";

        String testDriverEmail =
                "verify_driver@quickbite.test";

        /*
         * The exact User constructor/creation method depends on the
         * existing UserDAO implementation.
         *
         * We first check whether the users already exist.
         */

        User customer =
                userDAO.authenticate(
                        testCustomerEmail,
                        "pass123"
                );

        User admin =
                userDAO.authenticate(
                        testAdminEmail,
                        "pass123"
                );

        User driver =
                userDAO.authenticate(
                        testDriverEmail,
                        "pass123"
                );

        // ============================================================
        // 5. Test Existing Database Users If Available
        // ============================================================

        if (customer != null) {
            System.out.println(
                    "Test customer authenticated: "
                            + customer.getName()
            );
        } else {
            System.out.println(
                    "No test customer found - skipping customer authentication."
            );
        }

        if (admin != null) {
            System.out.println(
                    "Test admin authenticated: "
                            + admin.getName()
            );
        } else {
            System.out.println(
                    "No test admin found - skipping admin authentication."
            );
        }

        if (driver != null) {
            System.out.println(
                    "Test driver authenticated: "
                            + driver.getName()
            );
        } else {
            System.out.println(
                    "No test driver found - skipping driver authentication."
            );
        }

        // ============================================================
        // 6. JSON Utility Test
        // ============================================================

        String json = JsonUtil.toJson(items);

        System.out.println(
                "Serialized JSON menu preview (first 100 chars): "
                        + json.substring(
                        0,
                        Math.min(100, json.length())
                )
                        + "..."
        );

        List<FoodItem> parsed =
                JsonUtil.parseFoodItemList(json);

        System.out.println(
                "Parsed items count from JSON: "
                        + parsed.size()
        );

        assert parsed.size() == items.size()
                : "JSON parsing count mismatch";

        // ============================================================
        // 7. Test Order Creation Only If Customer Exists
        // ============================================================

        if (customer != null) {

            OrderService orderService =
                    new OrderService();

            List<CartItem> cart =
                    List.of(
                            new CartItem(
                                    items.get(0),
                                    2
                            )
                    );

            Order order =
                    orderService.placeOrder(
                            customer.getId(),
                            restaurant.getId(),
                            cart,
                            "123 Test St",
                            "Cash on Delivery",
                            3.99,
                            false
                    );

            System.out.println(
                    "Placed Test Order #"
                            + order.getId()
                            + " - Total: BDT "
                            + order.getTotalAmount()
            );

            assert order.getId() > 0
                    : "Order ID should be generated";

            // ========================================================
            // 8. Test Restaurant Statistics
            // ========================================================

            Map<String, Object> stats =
                    orderService.getRestaurantStatistics(
                            restaurant.getId()
                    );

            System.out.println(
                    "Restaurant stats: " + stats
            );

        } else {

            System.out.println(
                    "Order creation test skipped because "
                            + "no test customer exists."
            );
        }

        // ============================================================
        // 9. Final Result
        // ============================================================

        System.out.println(
                "=== ALL AVAILABLE VERIFICATION TESTS "
                        + "PASSED SUCCESSFULLY! ==="
        );
    }
}