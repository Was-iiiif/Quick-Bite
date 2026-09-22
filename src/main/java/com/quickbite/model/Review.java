package com.quickbite.model;

/**
 * Review entity model representing customer feedback and ratings.
 */
public class Review {
    private int id;
    private int userId;
    private int restaurantId;
    private int orderId;
    private int rating;
    private String comment;
    private String createdAt;

    // Display fields
    private String userName;
    private String restaurantName;

    public Review() {
    }

    public Review(int id, int userId, int restaurantId, int orderId, int rating, String comment, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.restaurantId = restaurantId;
        this.orderId = orderId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(int restaurantId) {
        this.restaurantId = restaurantId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }

    @Override
    public String toString() {
        return String.format("%d★ by %s: \"%s\"", rating, userName != null ? userName : "User #" + userId, comment);
    }
}
