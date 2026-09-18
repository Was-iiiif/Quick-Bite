package com.quickbite.dao;

import com.quickbite.config.DatabaseConfig;
import com.quickbite.model.Order;
import com.quickbite.model.OrderItem;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for Order and OrderItem entities.
 * Implements transaction management with auto-commit = false and rollback on error.
 */
public class OrderDAO {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Creates an order and all its child line items in an atomic database transaction.
     */
    public boolean createOrder(Order order) {
        String insertOrderSql = "INSERT INTO orders (user_id, restaurant_id, total_amount, delivery_fee, status, delivery_address, payment_method, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        String insertItemSql = "INSERT INTO order_items (order_id, food_id, quantity, unit_price) VALUES (?, ?, ?, ?);";

        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            String now = LocalDateTime.now().format(FORMATTER);
            order.setCreatedAt(now);

            try (PreparedStatement pstmt = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, order.getUserId());
                pstmt.setInt(2, order.getRestaurantId());
                pstmt.setDouble(3, order.getTotalAmount());
                pstmt.setDouble(4, order.getDeliveryFee());
                pstmt.setString(5, order.getStatus());
                pstmt.setString(6, order.getDeliveryAddress());
                pstmt.setString(7, order.getPaymentMethod());
                pstmt.setString(8, now);

                int affected = pstmt.executeUpdate();
                if (affected == 0) {
                    conn.rollback();
                    return false;
                }

                try (ResultSet gk = pstmt.getGeneratedKeys()) {
                    if (gk.next()) {
                        order.setId(gk.getInt(1));
                    } else {
                        conn.rollback();
                        return false;
                    }
                }
            }

            // Insert line items
            try (PreparedStatement itemStmt = conn.prepareStatement(insertItemSql, Statement.RETURN_GENERATED_KEYS)) {
                for (OrderItem item : order.getItems()) {
                    itemStmt.setInt(1, order.getId());
                    itemStmt.setInt(2, item.getFoodId());
                    itemStmt.setInt(3, item.getQuantity());
                    itemStmt.setDouble(4, item.getUnitPrice());
                    itemStmt.addBatch();
                }
                itemStmt.executeBatch();
            }

            conn.commit(); // Commit transaction
            return true;
        } catch (SQLException e) {
            System.err.println("OrderDAO createOrder transaction error: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("OrderDAO rollback error: " + ex.getMessage());
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
        return false;
    }

    public Order getById(int orderId) {
        String sql = """
            SELECT o.*, u.name as customer_name, u.phone as customer_phone, r.name as restaurant_name
            FROM orders o
            JOIN users u ON o.user_id = u.id
            JOIN restaurants r ON o.restaurant_id = r.id
            WHERE o.id = ?;
        """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Order order = mapRow(rs);
                    order.setItems(getOrderItems(conn, order.getId()));
                    return order;
                }
            }
        } catch (SQLException e) {
            System.err.println("OrderDAO getById error: " + e.getMessage());
        }
        return null;
    }

    public List<Order> getByUserId(int userId) {
        List<Order> list = new ArrayList<>();
        String sql = """
            SELECT o.*, u.name as customer_name, u.phone as customer_phone, r.name as restaurant_name
            FROM orders o
            JOIN users u ON o.user_id = u.id
            JOIN restaurants r ON o.restaurant_id = r.id
            WHERE o.user_id = ?
            ORDER BY o.id DESC;
        """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Order order = mapRow(rs);
                    order.setItems(getOrderItems(conn, order.getId()));
                    list.add(order);
                }
            }
        } catch (SQLException e) {
            System.err.println("OrderDAO getByUserId error: " + e.getMessage());
        }
        return list;
    }

    public List<Order> getByRestaurantId(int restaurantId) {
        List<Order> list = new ArrayList<>();
        String sql = """
            SELECT o.*, u.name as customer_name, u.phone as customer_phone, r.name as restaurant_name
            FROM orders o
            JOIN users u ON o.user_id = u.id
            JOIN restaurants r ON o.restaurant_id = r.id
            WHERE o.restaurant_id = ?
            ORDER BY o.id DESC;
        """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, restaurantId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Order order = mapRow(rs);
                    order.setItems(getOrderItems(conn, order.getId()));
                    list.add(order);
                }
            }
        } catch (SQLException e) {
            System.err.println("OrderDAO getByRestaurantId error: " + e.getMessage());
        }
        return list;
    }

    public boolean updateStatus(int orderId, String newStatus) {
        String sql = "UPDATE orders SET status = ? WHERE id = ?;";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, orderId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("OrderDAO updateStatus error: " + e.getMessage());
        }
        return false;
    }

    public Map<String, Object> getRevenueStatistics(int restaurantId) {
        Map<String, Object> stats = new HashMap<>();
        String sql = """
            SELECT 
                COUNT(*) as total_orders,
                SUM(CASE WHEN status != 'CANCELLED' THEN total_amount ELSE 0 END) as total_revenue,
                SUM(CASE WHEN status IN ('PLACED', 'CONFIRMED', 'PREPARING', 'READY', 'OUT_FOR_DELIVERY') THEN 1 ELSE 0 END) as active_orders,
                SUM(CASE WHEN status = 'DELIVERED' THEN 1 ELSE 0 END) as delivered_orders
            FROM orders
            WHERE restaurant_id = ?;
        """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, restaurantId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    stats.put("total_orders", rs.getInt("total_orders"));
                    stats.put("total_revenue", rs.getDouble("total_revenue"));
                    stats.put("active_orders", rs.getInt("active_orders"));
                    stats.put("delivered_orders", rs.getInt("delivered_orders"));
                }
            }
        } catch (SQLException e) {
            System.err.println("OrderDAO getRevenueStatistics error: " + e.getMessage());
        }
        return stats;
    }

    private List<OrderItem> getOrderItems(Connection conn, int orderId) throws SQLException {
        List<OrderItem> items = new ArrayList<>();
        String sql = """
            SELECT oi.*, fi.name as food_name
            FROM order_items oi
            JOIN food_items fi ON oi.food_id = fi.id
            WHERE oi.order_id = ?;
        """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new OrderItem(
                            rs.getInt("id"),
                            rs.getInt("order_id"),
                            rs.getInt("food_id"),
                            rs.getString("food_name"),
                            rs.getInt("quantity"),
                            rs.getDouble("unit_price")
                    ));
                }
            }
        }
        return items;
    }

    private Order mapRow(ResultSet rs) throws SQLException {
        Order order = new Order(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getInt("restaurant_id"),
                rs.getDouble("total_amount"),
                rs.getDouble("delivery_fee"),
                rs.getString("status"),
                rs.getString("delivery_address"),
                rs.getString("payment_method"),
                rs.getString("created_at")
        );
        order.setCustomerName(rs.getString("customer_name"));
        order.setCustomerPhone(rs.getString("customer_phone"));
        order.setRestaurantName(rs.getString("restaurant_name"));
        return order;
    }
}
