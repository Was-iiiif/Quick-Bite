package com.quickbite.util;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Seeds the database with rich, realistic mock data for testing, grading, and presentation.
 */
public class DatabaseSeeder {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void seedIfEmpty(Connection conn) throws SQLException {
        // Check if users table has records
        try (Statement checkStmt = conn.createStatement();
             ResultSet rs = checkStmt.executeQuery("SELECT COUNT(*) FROM users;")) {
            if (rs.next() && rs.getInt(1) > 0) {
                // Already seeded
                return;
            }
        }

        System.out.println("Seeding QuickBite database with initial sample data...");
        String now = LocalDateTime.now().format(FORMATTER);

        // 1. Insert Users
        String insertUserSql = "INSERT INTO users (name, email, password, phone, address, role, created_at) VALUES (?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(insertUserSql)) {
            // Customer
            pstmt.setString(1, "Alice Customer");
            pstmt.setString(2, "customer@quickbite.com");
            pstmt.setString(3, "pass123");
            pstmt.setString(4, "+1 555-0101");
            pstmt.setString(5, "742 Evergreen Terrace, Apt 4B");
            pstmt.setString(6, "CUSTOMER");
            pstmt.setString(7, now);
            pstmt.executeUpdate();

            // Restaurant Admin (Bella Italia)
            pstmt.setString(1, "Marco Rossi (Admin)");
            pstmt.setString(2, "admin@quickbite.com");
            pstmt.setString(3, "pass123");
            pstmt.setString(4, "+1 555-0202");
            pstmt.setString(5, "101 Little Italy Way");
            pstmt.setString(6, "RESTAURANT_ADMIN");
            pstmt.setString(7, now);
            pstmt.executeUpdate();

            // Delivery Staff 1
            pstmt.setString(1, "Dave Rider");
            pstmt.setString(2, "driver1@quickbite.com");
            pstmt.setString(3, "pass123");
            pstmt.setString(4, "+1 555-0301");
            pstmt.setString(5, "Downtown Logistics Hub #1");
            pstmt.setString(6, "DELIVERY_STAFF");
            pstmt.setString(7, now);
            pstmt.executeUpdate();

            // Delivery Staff 2
            pstmt.setString(1, "Elena Speed");
            pstmt.setString(2, "driver2@quickbite.com");
            pstmt.setString(3, "pass123");
            pstmt.setString(4, "+1 555-0302");
            pstmt.setString(5, "Metro Express Dispatch #2");
            pstmt.setString(6, "DELIVERY_STAFF");
            pstmt.setString(7, now);
            pstmt.executeUpdate();
        }

        // 2. Insert Restaurants
        String insertRestSql = "INSERT INTO restaurants (name, description, address, phone, rating, image_url) VALUES (?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(insertRestSql)) {
            // Restaurant 1
            pstmt.setString(1, "Bella Italia Trattoria");
            pstmt.setString(2, "Authentic stone-oven pizzas, handcrafted creamy pastas, and Italian desserts.");
            pstmt.setString(3, "101 Little Italy Way");
            pstmt.setString(4, "+1 555-1001");
            pstmt.setDouble(5, 4.8);
            pstmt.setString(6, "pizza.png");
            pstmt.executeUpdate();

            // Restaurant 2
            pstmt.setString(1, "Burger Bistro 99");
            pstmt.setString(2, "Gourmet smashed angus beef burgers, crispy waffle fries, and thick shakes.");
            pstmt.setString(3, "240 Main St, Downtown");
            pstmt.setString(4, "+1 555-1002");
            pstmt.setDouble(5, 4.7);
            pstmt.setString(6, "burger.png");
            pstmt.executeUpdate();

            // Restaurant 3
            pstmt.setString(1, "Tokyo Ramen & Bento");
            pstmt.setString(2, "Rich 16-hour tonkotsu broth, fresh handmade ramen noodles, and crispy pork gyoza.");
            pstmt.setString(3, "88 Sakura Boulevard");
            pstmt.setString(4, "+1 555-1003");
            pstmt.setDouble(5, 4.9);
            pstmt.setString(6, "ramen.png");
            pstmt.executeUpdate();

            // Restaurant 4
            pstmt.setString(1, "Green Garden Bowls");
            pstmt.setString(2, "Fresh organic superfood salads, wild salmon bowls, and refreshing cold-pressed juices.");
            pstmt.setString(3, "50 Market Square");
            pstmt.setString(4, "+1 555-1004");
            pstmt.setDouble(5, 4.6);
            pstmt.setString(6, "salad.png");
            pstmt.executeUpdate();
        }

        // 3. Insert Food Items
        String insertFoodSql = "INSERT INTO food_items (restaurant_id, name, description, category, price, available, image_url) VALUES (?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(insertFoodSql)) {
            // Restaurant 1: Bella Italia
            insertFood(pstmt, 1, "Margherita Stone Pizza", "San Marzano tomato sauce, fresh buffalo mozzarella, fresh basil & extra virgin olive oil.", "Pizza", 14.50, true);
            insertFood(pstmt, 1, "Truffle Tagliatelle", "Handcrafted tagliatelle with wild black truffles, aged parmesan cream, and parsley.", "Pasta", 18.00, true);
            insertFood(pstmt, 1, "Classic Beef Lasagna", "Layered slow-cooked bolognese ragù, creamy bechamel, and melted mozzarella.", "Pasta", 16.50, true);
            insertFood(pstmt, 1, "Garlic Herb Focaccia", "Warm rosemary focaccia baked with garlic butter and sea salt crystals.", "Sides", 6.00, true);
            insertFood(pstmt, 1, "Classic Tiramisu", "Espresso-soaked ladyfingers with rich mascarpone cream and dusted Belgian cocoa.", "Dessert", 8.50, true);

            // Restaurant 2: Burger Bistro 99
            insertFood(pstmt, 2, "Double Smashed Bacon Cheeseburger", "Twin smashed angus patties, smoked cheddar, crispy bacon, house pickles & secret sauce.", "Burgers", 13.99, true);
            insertFood(pstmt, 2, "Crispy Hot Honey Chicken Burger", "Buttermilk fried chicken breast, spicy honey glaze, creamy slaw on toasted brioche.", "Burgers", 12.50, true);
            insertFood(pstmt, 2, "Truffle Parmesan Waffle Fries", "Golden waffle-cut potatoes tossed in white truffle oil and grated pecorino.", "Sides", 6.50, true);
            insertFood(pstmt, 2, "Smoky Onion Rings", "Thick-cut beer-battered sweet yellow onions served with zesty BBQ dip.", "Sides", 5.50, true);
            insertFood(pstmt, 2, "Salted Caramel Shake", "Hand-spun vanilla bean ice cream blended with homemade salted caramel fudge.", "Drinks", 6.00, true);

            // Restaurant 3: Tokyo Ramen & Bento
            insertFood(pstmt, 3, "Signature Tonkotsu Ramen", "Creamy 16-hour pork bone broth, chashu pork belly, marinated ajitama egg, nori, scallions.", "Ramen", 15.50, true);
            insertFood(pstmt, 3, "Spicy Miso Ramen", "Rich roasted miso broth, spiced minced pork, bamboo shoots, sesame seeds and chili oil.", "Ramen", 16.00, true);
            insertFood(pstmt, 3, "Crispy Pork Gyoza (6 pcs)", "Pan-seared Japanese dumplings filled with seasoned pork and scallions, ponzu dip.", "Appetizers", 7.50, true);
            insertFood(pstmt, 3, "Chicken Katsu Bento Box", "Crispy panko chicken katsu with Japanese curry, steamed rice, pickled radish, salad.", "Bento", 17.50, true);
            insertFood(pstmt, 3, "Matcha Green Tea Cheesecake", "Silky ceremonial Uji matcha cheesecake on a buttery biscuit crust.", "Dessert", 7.00, true);

            // Restaurant 4: Green Garden Bowls
            insertFood(pstmt, 4, "Wild Salmon Quinoa Bowl", "Pan-roasted wild salmon, organic quinoa, avocado, edamame, and sesame ginger dressing.", "Bowls", 16.50, true);
            insertFood(pstmt, 4, "Mediterranean Falafel Salad", "Crispy spiced herb falafels, kalamata olives, cucumber, cherry tomatoes, and tahini drizzle.", "Salads", 13.00, true);
            insertFood(pstmt, 4, "Açai Berry Super Bowl", "Pure Amazon açai topped with hemp granola, sliced banana, strawberries, and chia seeds.", "Smoothies", 11.50, true);
            insertFood(pstmt, 4, "Cold-Pressed Green Detox Juice", "Cold-pressed kale, crisp cucumber, green apple, celery, and ginger root.", "Drinks", 6.50, true);
        }

        // 4. Insert an initial completed Order and Review for demonstration
        String pastOrderTime = LocalDateTime.now().minusHours(3).format(FORMATTER);
        String insertOrderSql = "INSERT INTO orders (user_id, restaurant_id, total_amount, delivery_fee, status, delivery_address, payment_method, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        int pastOrderId = 1;
        try (PreparedStatement pstmt = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, 1); // Alice Customer
            pstmt.setInt(2, 1); // Bella Italia
            pstmt.setDouble(3, 39.00); // 14.50 + 18.00 + 6.50 (fee)
            pstmt.setDouble(4, 6.50);
            pstmt.setString(5, "DELIVERED");
            pstmt.setString(6, "742 Evergreen Terrace, Apt 4B");
            pstmt.setString(7, "Credit Card");
            pstmt.setString(8, pastOrderTime);
            pstmt.executeUpdate();
            try (ResultSet gk = pstmt.getGeneratedKeys()) {
                if (gk.next()) pastOrderId = gk.getInt(1);
            }
        }

        // Order items for past order
        String insertItemSql = "INSERT INTO order_items (order_id, food_id, quantity, unit_price) VALUES (?, ?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(insertItemSql)) {
            pstmt.setInt(1, pastOrderId);
            pstmt.setInt(2, 1); // Margherita
            pstmt.setInt(3, 1);
            pstmt.setDouble(4, 14.50);
            pstmt.executeUpdate();

            pstmt.setInt(1, pastOrderId);
            pstmt.setInt(2, 2); // Truffle Tagliatelle
            pstmt.setInt(3, 1);
            pstmt.setDouble(4, 18.00);
            pstmt.executeUpdate();
        }

        // Delivery record for past order
        String insertDelivSql = "INSERT INTO deliveries (order_id, delivery_staff_id, status, assigned_at, picked_up_at, delivered_at) VALUES (?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(insertDelivSql)) {
            pstmt.setInt(1, pastOrderId);
            pstmt.setInt(2, 3); // Dave Rider
            pstmt.setString(3, "DELIVERED");
            pstmt.setString(4, LocalDateTime.now().minusHours(3).format(FORMATTER));
            pstmt.setString(5, LocalDateTime.now().minusHours(2).minusMinutes(30).format(FORMATTER));
            pstmt.setString(6, LocalDateTime.now().minusHours(2).format(FORMATTER));
            pstmt.executeUpdate();
        }

        // Review for past order
        String insertReviewSql = "INSERT INTO reviews (user_id, restaurant_id, order_id, rating, comment, created_at) VALUES (?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(insertReviewSql)) {
            pstmt.setInt(1, 1); // Alice
            pstmt.setInt(2, 1); // Bella Italia
            pstmt.setInt(3, pastOrderId);
            pstmt.setInt(4, 5);
            pstmt.setString(5, "The pizza crust was perfectly crispy and the truffle pasta was incredible! Arrived warm.");
            pstmt.setString(6, LocalDateTime.now().minusHours(1).format(FORMATTER));
            pstmt.executeUpdate();
        }

        System.out.println("QuickBite initial seeding completed successfully!");
    }

    private static void insertFood(PreparedStatement pstmt, int restId, String name, String desc, String cat, double price, boolean avail) throws SQLException {
        pstmt.setInt(1, restId);
        pstmt.setString(2, name);
        pstmt.setString(3, desc);
        pstmt.setString(4, cat);
        pstmt.setDouble(5, price);
        pstmt.setInt(6, avail ? 1 : 0);
        pstmt.setString(7, cat.toLowerCase() + ".png");
        pstmt.executeUpdate();
    }
}
