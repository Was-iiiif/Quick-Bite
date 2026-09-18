package com.quickbite.dao;

import com.quickbite.config.DatabaseConfig;
import com.quickbite.model.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for User entities.
 * Utilizes PreparedStatements to prevent SQL injection and map relational records to OOP hierarchy.
 */
public class UserDAO {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public User authenticate(String email, String password) {
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?) AND password = ?;";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email.trim());
            pstmt.setString(2, password.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("UserDAO authenticate error: " + e.getMessage());
        }
        return null;
    }

    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?);";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("UserDAO findByEmail error: " + e.getMessage());
        }
        return null;
    }

    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?;";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("UserDAO findById error: " + e.getMessage());
        }
        return null;
    }

    public boolean create(User user) {
        String sql = "INSERT INTO users (name, email, password, phone, address, role, created_at) VALUES (?, ?, ?, ?, ?, ?, ?);";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            String now = LocalDateTime.now().format(FORMATTER);
            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getEmail().trim().toLowerCase());
            pstmt.setString(3, user.getPassword());
            pstmt.setString(4, user.getPhone());
            pstmt.setString(5, user.getAddress());
            pstmt.setString(6, user.getRole());
            pstmt.setString(7, now);

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet gk = pstmt.getGeneratedKeys()) {
                    if (gk.next()) {
                        user.setId(gk.getInt(1));
                        user.setCreatedAt(now);
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("UserDAO create error: " + e.getMessage());
        }
        return false;
    }

    public List<DeliveryStaff> getAllDeliveryStaff() {
        List<DeliveryStaff> list = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = 'DELIVERY_STAFF';";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add((DeliveryStaff) mapRowToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("UserDAO getAllDeliveryStaff error: " + e.getMessage());
        }
        return list;
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        String password = rs.getString("password");
        String phone = rs.getString("phone");
        String address = rs.getString("address");
        String role = rs.getString("role");
        String createdAt = rs.getString("created_at");

        if ("CUSTOMER".equalsIgnoreCase(role)) {
            return new Customer(id, name, email, password, phone, address, createdAt);
        } else if ("RESTAURANT_ADMIN".equalsIgnoreCase(role)) {
            // By default, map admin to restaurant 1 (Bella Italia) or id
            int restId = 1;
            return new RestaurantAdmin(id, name, email, password, phone, address, createdAt, restId);
        } else if ("DELIVERY_STAFF".equalsIgnoreCase(role)) {
            return new DeliveryStaff(id, name, email, password, phone, address, createdAt, true, "Motorbike");
        }

        return new Customer(id, name, email, password, phone, address, createdAt);
    }
}
