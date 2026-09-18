package com.quickbite.dao;

import com.quickbite.config.DatabaseConfig;
import com.quickbite.model.Delivery;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Delivery entity.
 * Handles tracking, assignment, and status updates for delivery personnel.
 */
public class DeliveryDAO {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public boolean createOrAssignDelivery(int orderId, int driverId) {
        String checkSql = "SELECT id FROM deliveries WHERE order_id = ?;";
        String insertSql = "INSERT INTO deliveries (order_id, delivery_staff_id, status, assigned_at) VALUES (?, ?, ?, ?);";
        String updateSql = "UPDATE deliveries SET delivery_staff_id = ?, status = ?, assigned_at = ? WHERE order_id = ?;";

        try (Connection conn = DatabaseConfig.getConnection()) {
            boolean exists = false;
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, orderId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    exists = rs.next();
                }
            }

            String now = LocalDateTime.now().format(FORMATTER);
            if (exists) {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, driverId);
                    updateStmt.setString(2, Delivery.STATUS_ASSIGNED);
                    updateStmt.setString(3, now);
                    updateStmt.setInt(4, orderId);
                    return updateStmt.executeUpdate() > 0;
                }
            } else {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setInt(1, orderId);
                    insertStmt.setInt(2, driverId);
                    insertStmt.setString(3, Delivery.STATUS_ASSIGNED);
                    insertStmt.setString(4, now);
                    return insertStmt.executeUpdate() > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("DeliveryDAO createOrAssignDelivery error: " + e.getMessage());
        }
        return false;
    }

    public Delivery getByOrderId(int orderId) {
        String sql = """
            SELECT d.*, u.name as driver_name, u.phone as driver_phone,
                   cu.name as customer_name, o.delivery_address, r.name as restaurant_name, o.total_amount
            FROM deliveries d
            LEFT JOIN users u ON d.delivery_staff_id = u.id
            JOIN orders o ON d.order_id = o.id
            JOIN users cu ON o.user_id = cu.id
            JOIN restaurants r ON o.restaurant_id = r.id
            WHERE d.order_id = ?;
        """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("DeliveryDAO getByOrderId error: " + e.getMessage());
        }
        return null;
    }

    public List<Delivery> getDeliveriesByStaffId(int staffId) {
        List<Delivery> list = new ArrayList<>();
        String sql = """
            SELECT d.*, u.name as driver_name, u.phone as driver_phone,
                   cu.name as customer_name, o.delivery_address, r.name as restaurant_name, o.total_amount
            FROM deliveries d
            LEFT JOIN users u ON d.delivery_staff_id = u.id
            JOIN orders o ON d.order_id = o.id
            JOIN users cu ON o.user_id = cu.id
            JOIN restaurants r ON o.restaurant_id = r.id
            WHERE d.delivery_staff_id = ?
            ORDER BY d.id DESC;
        """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, staffId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("DeliveryDAO getDeliveriesByStaffId error: " + e.getMessage());
        }
        return list;
    }

    public List<Delivery> getAllActiveDeliveries() {
        List<Delivery> list = new ArrayList<>();
        String sql = """
            SELECT d.*, u.name as driver_name, u.phone as driver_phone,
                   cu.name as customer_name, o.delivery_address, r.name as restaurant_name, o.total_amount
            FROM deliveries d
            LEFT JOIN users u ON d.delivery_staff_id = u.id
            JOIN orders o ON d.order_id = o.id
            JOIN users cu ON o.user_id = cu.id
            JOIN restaurants r ON o.restaurant_id = r.id
            WHERE d.status != 'DELIVERED' AND d.status != 'CANCELLED'
            ORDER BY d.id DESC;
        """;
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("DeliveryDAO getAllActiveDeliveries error: " + e.getMessage());
        }
        return list;
    }

    public boolean updateStatus(int deliveryId, String newStatus) {
        String now = LocalDateTime.now().format(FORMATTER);
        String sql;
        if (Delivery.STATUS_PICKED_UP.equals(newStatus)) {
            sql = "UPDATE deliveries SET status = ?, picked_up_at = ? WHERE id = ?;";
        } else if (Delivery.STATUS_DELIVERED.equals(newStatus)) {
            sql = "UPDATE deliveries SET status = ?, delivered_at = ? WHERE id = ?;";
        } else {
            sql = "UPDATE deliveries SET status = ? WHERE id = ?;";
        }

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            if (Delivery.STATUS_PICKED_UP.equals(newStatus) || Delivery.STATUS_DELIVERED.equals(newStatus)) {
                pstmt.setString(2, now);
                pstmt.setInt(3, deliveryId);
            } else {
                pstmt.setInt(2, deliveryId);
            }
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("DeliveryDAO updateStatus error: " + e.getMessage());
        }
        return false;
    }

    private Delivery mapRow(ResultSet rs) throws SQLException {
        Delivery delivery = new Delivery(
                rs.getInt("id"),
                rs.getInt("order_id"),
                rs.getInt("delivery_staff_id"),
                rs.getString("status"),
                rs.getString("assigned_at"),
                rs.getString("picked_up_at"),
                rs.getString("delivered_at")
        );
        delivery.setDriverName(rs.getString("driver_name"));
        delivery.setDriverPhone(rs.getString("driver_phone"));
        delivery.setCustomerName(rs.getString("customer_name"));
        delivery.setDeliveryAddress(rs.getString("delivery_address"));
        delivery.setRestaurantName(rs.getString("restaurant_name"));
        delivery.setOrderTotal(rs.getDouble("total_amount"));
        return delivery;
    }
}
