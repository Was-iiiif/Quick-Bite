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
        System.out.println("Checking QuickBite database seed status...");
        // Check if restaurants or users table has records
        try (Statement checkStmt = conn.createStatement();
             ResultSet rs = checkStmt.executeQuery("SELECT COUNT(*) FROM restaurants;")) {
            if (rs.next() && rs.getInt(1) > 0) {
                // Already seeded and customized
                return;
            }
        }
        try (Statement checkStmt = conn.createStatement();
             ResultSet rs = checkStmt.executeQuery("SELECT COUNT(*) FROM users;")) {
            if (rs.next() && rs.getInt(1) > 0) {
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

    private static void ensureFigmaRestaurants(Connection conn) {
        try {
            // Check if "Ember & Ash" already exists
            try (PreparedStatement check = conn.prepareStatement("SELECT COUNT(*) FROM restaurants WHERE name = ?;")) {
                check.setString(1, "Ember & Ash");
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return; // Already present
                    }
                }
            }

            System.out.println("Seeding Figma restaurant partners and menu catalogs...");

            String insertRest = "INSERT INTO restaurants (name, description, address, phone, rating, image_url) VALUES (?, ?, ?, ?, ?, ?);";
            int emberId = insertSingleRest(conn, insertRest, "Ember & Ash", "Wood-fired Pizza & Artisanal Italian Delicacies", "104 Sullivan St, SoHo", "+1 555-4821", 4.9, "pizza.png");
            int shogunId = insertSingleRest(conn, insertRest, "Shogun Omakase", "Premium Sushi & Traditional Japanese Cuisine", "52 Mercer St, Tribeca", "+1 555-8820", 4.8, "sushi.png");
            int pattyId = insertSingleRest(conn, insertRest, "The Patty Lab", "Craft Smashed Burgers & Loaded Truffle Fries", "318 Bleecker St, West Village", "+1 555-3400", 4.7, "burger.png");
            int lemonId = insertSingleRest(conn, insertRest, "Lemongrass House", "Authentic Thai Street Food & Fragrant Curries", "77 Mulberry St, Chinatown", "+1 555-5430", 4.6, "thai.png");
            int fieldId = insertSingleRest(conn, insertRest, "Field & Fork", "Garden Salads, Grain Bowls & Cold-Pressed Juices", "210 Spring St, Hudson Square", "+1 555-3890", 4.5, "salad.png");
            int petiteId = insertSingleRest(conn, insertRest, "Petite Maison", "Artisan French Bakery & Decadent Desserts", "142 Prince St, SoHo", "+1 555-7180", 4.9, "dessert.png");

            String insertFoodSql = "INSERT INTO food_items (restaurant_id, name, description, category, price, available, image_url) VALUES (?, ?, ?, ?, ?, ?, ?);";
            try (PreparedStatement pstmt = conn.prepareStatement(insertFoodSql)) {
                // Ember & Ash
                insertFood(pstmt, emberId, "Truffle Funghi", "Wild forest mushrooms, black truffle cream, buffalo mozzarella, fresh thyme.", "Pizza", 26.50, true);
                insertFood(pstmt, emberId, "Margherita Classica", "San Marzano tomatoes, fresh buffalo mozzarella, aromatic basil, EVOO.", "Pizza", 18.50, true);
                insertFood(pstmt, emberId, "Burrata & Prosciutto", "Pugliese burrata, 24-month aged di Parma prosciutto, aged balsamic glaze.", "Appetizers", 16.00, true);
                insertFood(pstmt, emberId, "Classic Tiramisu", "Savoiardi ladyfingers, rich mascarpone cream, dark espresso, cocoa dust.", "Desserts", 9.50, true);

                // Shogun Omakase
                insertFood(pstmt, shogunId, "Toro Nigiri (4 pcs)", "Premium fatty bluefin tuna with nikiri soy and freshly grated wasabi.", "Sushi", 38.00, true);
                insertFood(pstmt, shogunId, "Miso Soup", "Organic red & white miso, bonito dashi broth, silken tofu, scallions.", "Appetizers", 6.00, true);
                insertFood(pstmt, shogunId, "Dragon Roll", "BBQ freshwater eel, cucumber, avocado, orange tobiko, unagi glaze.", "Sushi", 22.00, true);
                insertFood(pstmt, shogunId, "Salmon Sashimi Platter", "Fresh Scottish king salmon, daikon radish, pickled young ginger.", "Sushi", 26.00, true);

                // The Patty Lab
                insertFood(pstmt, pattyId, "Double Smash Burger", "Twin smashed angus beef patties, smoked cheddar, house pickles & secret sauce.", "Burgers", 16.00, true);
                insertFood(pstmt, pattyId, "Truffle Fries", "Crispy skin-on fries, white truffle oil, shaved pecorino, chives.", "Sides", 8.00, true);
                insertFood(pstmt, pattyId, "Crispy Hot Honey Chicken", "Buttermilk fried chicken breast, Nashville hot honey glaze, creamy slaw.", "Burgers", 14.50, true);
                insertFood(pstmt, pattyId, "Craft Salted Caramel Shake", "Handcrafted vanilla bean ice cream, homemade sea salt caramel fudge.", "Drinks", 7.50, true);

                // Lemongrass House
                insertFood(pstmt, lemonId, "Pad Thai Noodles", "Stir-fried rice noodles, tiger prawns, pressed tofu, bean sprouts, crushed peanuts.", "Thai", 16.50, true);
                insertFood(pstmt, lemonId, "Tom Yum Goong", "Spicy and sour lemongrass broth with giant prawns, wild mushrooms, kaffir lime.", "Thai", 12.00, true);
                insertFood(pstmt, lemonId, "Green Curry Chicken", "Coconut milk, Thai sweet basil, tender chicken, bamboo shoots, jasmine rice.", "Thai", 17.00, true);
                insertFood(pstmt, lemonId, "Crispy Vegetable Spring Rolls", "Handmade rolls filled with glass noodles, cabbage, carrots, sweet chili dip.", "Sides", 7.00, true);

                // Field & Fork
                insertFood(pstmt, fieldId, "Wild Salmon Quinoa Bowl", "Pan-seared wild salmon, organic red quinoa, ripe avocado, edamame, sesame dressing.", "Salads", 18.50, true);
                insertFood(pstmt, fieldId, "Mediterranean Falafel Salad", "Crispy spiced herb falafels, kalamata olives, heirloom cherry tomatoes, tahini.", "Salads", 14.00, true);
                insertFood(pstmt, fieldId, "Açai Berry Super Bowl", "Pure organic Amazon açai, hemp seed granola, fresh banana, chia seeds.", "Salads", 12.50, true);
                insertFood(pstmt, fieldId, "Cold-Pressed Green Detox", "Organic kale, green apple, cucumber, celery, ginger root.", "Drinks", 7.00, true);

                // Petite Maison
                insertFood(pstmt, petiteId, "French Strawberry Parfait", "Layered sweet vanilla cream, fresh strawberries, butter biscuit crumble.", "Desserts", 11.00, true);
                insertFood(pstmt, petiteId, "Pistachio & Rose Macarons (6 pcs)", "Handcrafted almond flour shells, rich pistachio buttercream filling.", "Desserts", 14.00, true);
                insertFood(pstmt, petiteId, "Classic Vanilla Crème Brûlée", "Rich vanilla bean custard base topped with crackling caramelized sugar.", "Desserts", 10.50, true);
                insertFood(pstmt, petiteId, "Warm Molten Chocolate Fondant", "Guanaja 70% dark chocolate cake with flowing warm truffle center.", "Desserts", 12.00, true);
            }

            System.out.println("Figma restaurant partners seeded successfully.");
        } catch (Exception ex) {
            System.err.println("Note on Figma seeding: " + ex.getMessage());
        }
    }

    private static int insertSingleRest(Connection conn, String sql, String name, String desc, String address, String phone, double rating, String img) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name);
            pstmt.setString(2, desc);
            pstmt.setString(3, address);
            pstmt.setString(4, phone);
            pstmt.setDouble(5, rating);
            pstmt.setString(6, img);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 1;
    }
}

