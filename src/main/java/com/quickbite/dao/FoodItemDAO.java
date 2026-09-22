package com.quickbite.dao;

import com.quickbite.config.DatabaseConfig;
import com.quickbite.model.FoodItem;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for FoodItem entity.
 * Supports Full CRUD: Create, Read, Update, Delete, and availability toggle.
 */
public class FoodItemDAO {

    public List<FoodItem> getByRestaurantId(int restaurantId) {
        List<FoodItem> list = new ArrayList<>();
        String sql = "SELECT * FROM food_items WHERE restaurant_id = ? ORDER BY category ASC, name ASC;";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, restaurantId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("FoodItemDAO getByRestaurantId error: " + e.getMessage());
        }
        return list;
    }

    public FoodItem getById(int id) {
        String sql = "SELECT * FROM food_items WHERE id = ?;";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("FoodItemDAO getById error: " + e.getMessage());
        }
        return null;
    }

    public boolean create(FoodItem item) {
        String sql = "INSERT INTO food_items (restaurant_id, name, description, category, price, available, image_url) VALUES (?, ?, ?, ?, ?, ?, ?);";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, item.getRestaurantId());
            pstmt.setString(2, item.getName());
            pstmt.setString(3, item.getDescription());
            pstmt.setString(4, item.getCategory());
            pstmt.setDouble(5, item.getPrice());
            pstmt.setInt(6, item.isAvailable() ? 1 : 0);
            pstmt.setString(7, item.getImageUrl() != null ? item.getImageUrl() : "food.png");

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet gk = pstmt.getGeneratedKeys()) {
                    if (gk.next()) {
                        item.setId(gk.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("FoodItemDAO create error: " + e.getMessage());
        }
        return false;
    }

    public boolean update(FoodItem item) {
        String sql = "UPDATE food_items SET name = ?, description = ?, category = ?, price = ?, available = ?, image_url = ? WHERE id = ?;";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, item.getName());
            pstmt.setString(2, item.getDescription());
            pstmt.setString(3, item.getCategory());
            pstmt.setDouble(4, item.getPrice());
            pstmt.setInt(5, item.isAvailable() ? 1 : 0);
            pstmt.setString(6, item.getImageUrl());
            pstmt.setInt(7, item.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("FoodItemDAO update error: " + e.getMessage());
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM food_items WHERE id = ?;";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("FoodItemDAO delete error: " + e.getMessage());
        }
        return false;
    }

    public boolean toggleAvailability(int id, boolean available) {
        String sql = "UPDATE food_items SET available = ? WHERE id = ?;";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, available ? 1 : 0);
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("FoodItemDAO toggleAvailability error: " + e.getMessage());
        }
        return false;
    }

    public List<String> getCategories(int restaurantId) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM food_items WHERE restaurant_id = ? ORDER BY category ASC;";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, restaurantId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("category"));
                }
            }
        } catch (SQLException e) {
            System.err.println("FoodItemDAO getCategories error: " + e.getMessage());
        }
        return list;
    }

    private FoodItem mapRow(ResultSet rs) throws SQLException {
        return new FoodItem(
                rs.getInt("id"),
                rs.getInt("restaurant_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("category"),
                rs.getDouble("price"),
                rs.getInt("available") == 1,
                rs.getString("image_url")
        );
    }
}
