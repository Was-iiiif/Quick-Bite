package com.quickbite.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Order entity representing customer food orders and their full lifecycle states:
 * PLACED -> CONFIRMED -> PREPARING -> READY -> OUT_FOR_DELIVERY -> DELIVERED (or CANCELLED).
 */
public class Order {
    public static final String STATUS_PLACED = "PLACED";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_PREPARING = "PREPARING";
    public static final String STATUS_READY = "READY";
    public static final String STATUS_OUT_FOR_DELIVERY = "OUT_FOR_DELIVERY";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private int id;
    private int userId;
    private int restaurantId;
    private double totalAmount;
    private double deliveryFee;
    private String status;
    private String deliveryAddress;
    private String paymentMethod;
    private String createdAt;

    // Additional display & relational fields
    private String customerName;
    private String customerPhone;
    private String restaurantName;
    private List<OrderItem> items = new ArrayList<>();
    private SmartDeliveryInfo smartDeliveryInfo;

    public Order() {
        this.status = STATUS_PLACED;
    }

    public Order(int id, int userId, int restaurantId, double totalAmount, double deliveryFee, String status, String deliveryAddress, String paymentMethod, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.restaurantId = restaurantId;
        this.totalAmount = totalAmount;
        this.deliveryFee = deliveryFee;
        this.status = status;
        this.deliveryAddress = deliveryAddress;
        this.paymentMethod = paymentMethod;
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

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public double getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(double deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public SmartDeliveryInfo getSmartDeliveryInfo() {
        return smartDeliveryInfo;
    }

    public void setSmartDeliveryInfo(SmartDeliveryInfo smartDeliveryInfo) {
        this.smartDeliveryInfo = smartDeliveryInfo;
    }

    public double getSubtotal() {
        return Math.max(0.0, totalAmount - deliveryFee);
    }

    @Override
    public String toString() {
        return String.format("Order #%d [%s] - BDT %.2f (%s)", id, status, totalAmount, restaurantName != null ? restaurantName : "Restaurant #" + restaurantId);
    }
}
