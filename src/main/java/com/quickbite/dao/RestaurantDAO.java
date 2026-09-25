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

    public List<Restaurant> getByOwner(int adminId) {
        List<Restaurant> list = new ArrayList<>();
        String sql = "SELECT * FROM restaurants WHERE owner_admin_id = ? ORDER BY rating DESC, name ASC;";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, adminId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("RestaurantDAO getByOwner error: " + e.getMessage());
        }
        return list;
    }

    public boolean create(Restaurant r) {
        String sql = "INSERT INTO restaurants (name, description, address, phone, rating, image_url, owner_admin_id) VALUES (?, ?, ?, ?, ?, ?, ?);";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, r.getName());
            pstmt.setString(2, r.getDescription());
            pstmt.setString(3, r.getAddress());
            pstmt.setString(4, r.getPhone());
            pstmt.setDouble(5, r.getRating() > 0 ? r.getRating() : 4.5);
            pstmt.setString(6, r.getImageUrl() != null ? r.getImageUrl() : "pizza.png");
            pstmt.setInt(7, r.getOwnerAdminId());
            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) r.setId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("RestaurantDAO create error: " + e.getMessage());
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM restaurants WHERE id = ?;";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("RestaurantDAO delete error: " + e.getMessage());
        }
        return false;
    }

    public boolean update(Restaurant r) {
        String sql = "UPDATE restaurants SET name = ?, description = ?, address = ?, phone = ?, rating = ?, image_url = ? WHERE id = ?;";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, r.getName());
            pstmt.setString(2, r.getDescription());
            pstmt.setString(3, r.getAddress());
            pstmt.setString(4, r.getPhone());
            pstmt.setDouble(5, r.getRating());
            pstmt.setString(6, r.getImageUrl());
            pstmt.setInt(7, r.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("RestaurantDAO update error: " + e.getMessage());
        }
        return false;
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
        Restaurant r = new Restaurant(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("address"),
                rs.getString("phone"),
                rs.getDouble("rating"),
                rs.getString("image_url")
        );
        r.setOwnerAdminId(rs.getInt("owner_admin_id"));
        return r;
    }
}
