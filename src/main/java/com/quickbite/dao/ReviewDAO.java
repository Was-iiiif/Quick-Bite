package com.quickbite.dao;

import com.quickbite.config.DatabaseConfig;
import com.quickbite.model.Review;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for customer Reviews and Ratings.
 */
public class ReviewDAO {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public boolean create(Review review) {
        String sql = "INSERT INTO reviews (user_id, restaurant_id, order_id, rating, comment, created_at) VALUES (?, ?, ?, ?, ?, ?);";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            String now = LocalDateTime.now().format(FORMATTER);
            pstmt.setInt(1, review.getUserId());
            pstmt.setInt(2, review.getRestaurantId());
            pstmt.setInt(3, review.getOrderId());
            pstmt.setInt(4, review.getRating());
            pstmt.setString(5, review.getComment());
            pstmt.setString(6, now);

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet gk = pstmt.getGeneratedKeys()) {
                    if (gk.next()) {
                        review.setId(gk.getInt(1));
                        review.setCreatedAt(now);
                    }
                }
                // Also update restaurant's average rating
                updateRestaurantRating(conn, review.getRestaurantId());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("ReviewDAO create error: " + e.getMessage());
        }
        return false;
    }

    public List<Review> getByRestaurantId(int restaurantId) {
        List<Review> list = new ArrayList<>();
        String sql = """
            SELECT r.*, u.name as user_name, rest.name as restaurant_name
            FROM reviews r
            JOIN users u ON r.user_id = u.id
            JOIN restaurants rest ON r.restaurant_id = rest.id
            WHERE r.restaurant_id = ?
            ORDER BY r.id DESC;
        """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, restaurantId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("ReviewDAO getByRestaurantId error: " + e.getMessage());
        }
        return list;
    }

    public boolean hasUserReviewedOrder(int userId, int orderId) {
        String sql = "SELECT id FROM reviews WHERE user_id = ? AND order_id = ?;";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("ReviewDAO hasUserReviewedOrder error: " + e.getMessage());
        }
        return false;
    }

    private void updateRestaurantRating(Connection conn, int restaurantId) {
        String calcSql = "SELECT AVG(rating) as avg_rating FROM reviews WHERE restaurant_id = ?;";
        String updateSql = "UPDATE restaurants SET rating = ? WHERE id = ?;";
        try (PreparedStatement calcStmt = conn.prepareStatement(calcSql)) {
            calcStmt.setInt(1, restaurantId);
            try (ResultSet rs = calcStmt.executeQuery()) {
                if (rs.next()) {
                    double avg = rs.getDouble("avg_rating");
                    if (avg > 0) {
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setDouble(1, Math.round(avg * 10.0) / 10.0);
                            updateStmt.setInt(2, restaurantId);
                            updateStmt.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("ReviewDAO updateRestaurantRating error: " + e.getMessage());
        }
    }

    private Review mapRow(ResultSet rs) throws SQLException {
        Review review = new Review(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getInt("restaurant_id"),
                rs.getInt("order_id"),
                rs.getInt("rating"),
                rs.getString("comment"),
                rs.getString("created_at")
        );
        review.setUserName(rs.getString("user_name"));
        review.setRestaurantName(rs.getString("restaurant_name"));
        return review;
    }
}
