package com.quickbite.dao;

import com.quickbite.config.DatabaseConfig;
import com.quickbite.model.Restaurant;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Restaurant entity.
 */
public class RestaurantDAO {

    public List<Restaurant> getAll() {
        List<Restaurant> list = new ArrayList<>();
        String sql = "SELECT * FROM restaurants ORDER BY rating DESC, name ASC;";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("RestaurantDAO getAll error: " + e.getMessage());
        }
        return list;
    }

    public Restaurant getById(int id) {
        String sql = "SELECT * FROM restaurants WHERE id = ?;";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("RestaurantDAO getById error: " + e.getMessage());
        }
        return null;
    }

    public List<Restaurant> search(String keyword) {
        List<Restaurant> list = new ArrayList<>();
        String sql = "SELECT * FROM restaurants WHERE LOWER(name) LIKE ? OR LOWER(description) LIKE ?;";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String term = "%" + keyword.toLowerCase().trim() + "%";
            pstmt.setString(1, term);
            pstmt.setString(2, term);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("RestaurantDAO search error: " + e.getMessage());
        }
        return list;
    }

    public boolean updateRating(int id, double newRating) {
        String sql = "UPDATE restaurants SET rating = ? WHERE id = ?;";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, Math.round(newRating * 10.0) / 10.0);
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("RestaurantDAO updateRating error: " + e.getMessage());
        }
        return false;
    }

    private Restaurant mapRow(ResultSet rs) throws SQLException {
        return new Restaurant(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("address"),
                rs.getString("phone"),
                rs.getDouble("rating"),
                rs.getString("image_url")
        );
    }
}
