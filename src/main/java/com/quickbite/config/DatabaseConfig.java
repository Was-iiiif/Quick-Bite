package com.quickbite.config;

import com.quickbite.util.DatabaseSeeder;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database configuration and connection manager for SQLite via JDBC.
 * Automatically initializes schema and seeds initial data if database is empty.
 */
public class DatabaseConfig {
    private static final String DB_DIR = "database";
    private static final String DB_FILE = "QuickBite.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_DIR + "/" + DB_FILE;

    static {
        try {
            // Ensure SQLite JDBC Driver is registered
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Warning: org.sqlite.JDBC driver not found on classpath: " + e.getMessage());
        }
    }

    /**
     * Establishes and returns a new connection to the SQLite database.
     * Enforces foreign key constraints.
     */
    public static Connection getConnection() throws SQLException {
        ensureDirectoryExists();
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    /**
     * Creates directory if it doesn't exist.
     */
    private static void ensureDirectoryExists() {
        File dir = new File(DB_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Initializes the relational database schema according to the design specification:
     * - users
     * - restaurants
     * - food_items
     * - orders
     * - order_items
     * - deliveries
     * - reviews
     */
    public static void initializeDatabase() {
        ensureDirectoryExists();
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // 1. Users Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    email TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL,
                    phone TEXT,
                    address TEXT,
                    role TEXT NOT NULL,
                    created_at TEXT NOT NULL
                );
            """);

            // 2. Restaurants Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS restaurants (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    description TEXT,
                    address TEXT NOT NULL,
                    phone TEXT,
                    rating REAL DEFAULT 5.0,
                    image_url TEXT
                );
            """);

            // 3. Food Items Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS food_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    restaurant_id INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    description TEXT,
                    category TEXT NOT NULL,
                    price REAL NOT NULL,
                    available INTEGER NOT NULL DEFAULT 1,
                    image_url TEXT,
                    FOREIGN KEY (restaurant_id) REFERENCES restaurants (id) ON DELETE CASCADE
                );
            """);

            // 4. Orders Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS orders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    restaurant_id INTEGER NOT NULL,
                    total_amount REAL NOT NULL,
                    delivery_fee REAL NOT NULL,
                    status TEXT NOT NULL,
                    delivery_address TEXT NOT NULL,
                    payment_method TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                    FOREIGN KEY (restaurant_id) REFERENCES restaurants (id) ON DELETE CASCADE
                );
            """);

            // 5. Order Items Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS order_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    order_id INTEGER NOT NULL,
                    food_id INTEGER NOT NULL,
                    quantity INTEGER NOT NULL,
                    unit_price REAL NOT NULL,
                    FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
                    FOREIGN KEY (food_id) REFERENCES food_items (id) ON DELETE CASCADE
                );
            """);

            // 6. Deliveries Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS deliveries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    order_id INTEGER UNIQUE NOT NULL,
                    delivery_staff_id INTEGER,
                    status TEXT NOT NULL,
                    assigned_at TEXT,
                    picked_up_at TEXT,
                    delivered_at TEXT,
                    FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
                    FOREIGN KEY (delivery_staff_id) REFERENCES users (id) ON DELETE SET NULL
                );
            """);

            // 7. Reviews Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reviews (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    restaurant_id INTEGER NOT NULL,
                    order_id INTEGER NOT NULL,
                    rating INTEGER NOT NULL,
                    comment TEXT,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                    FOREIGN KEY (restaurant_id) REFERENCES restaurants (id) ON DELETE CASCADE,
                    FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
                );
            """);

            // Seed default data if needed
            DatabaseSeeder.seedIfEmpty(conn);

            System.out.println("QuickBite SQLite Database successfully initialized at: " + DB_URL);
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        initializeDatabase();
    }
}

