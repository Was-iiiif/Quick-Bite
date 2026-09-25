package com.quickbite.util;

import java.sql.*;

/**
 * Database seeder — intentionally empty.
 * All users, restaurants, and delivery staff are created manually through the UI.
 */
public class DatabaseSeeder {

    public static void seedIfEmpty(Connection conn) throws SQLException {
        // No automatic seeding — the database starts completely empty.
        // Users, restaurants, and delivery staff must be created via the application UI.
        System.out.println("QuickBite: starting with empty database (no seed data).");
    }

}