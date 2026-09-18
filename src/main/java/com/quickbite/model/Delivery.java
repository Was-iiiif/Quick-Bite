package com.quickbite.model;

/**
 * Delivery entity tracking order transport by delivery staff.
 */
public class Delivery {
    public static final String STATUS_ASSIGNED = "ASSIGNED";
    public static final String STATUS_PICKED_UP = "PICKED_UP";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private int id;
    private int orderId;
    private int deliveryStaffId;
    private String status;
    private String assignedAt;
    private String pickedUpAt;
    private String deliveredAt;

    // Display / join fields
    private String driverName;
    private String driverPhone;
    private String customerName;
    private String deliveryAddress;
    private String restaurantName;
    private double orderTotal;

    public Delivery() {
        this.status = STATUS_ASSIGNED;
    }

    public Delivery(int id, int orderId, int deliveryStaffId, String status, String assignedAt, String pickedUpAt, String deliveredAt) {
        this.id = id;
        this.orderId = orderId;
        this.deliveryStaffId = deliveryStaffId;
        this.status = status;
        this.assignedAt = assignedAt;
        this.pickedUpAt = pickedUpAt;
        this.deliveredAt = deliveredAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getDeliveryStaffId() {
        return deliveryStaffId;
    }

    public void setDeliveryStaffId(int deliveryStaffId) {
        this.deliveryStaffId = deliveryStaffId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(String assignedAt) {
        this.assignedAt = assignedAt;
    }

    public String getPickedUpAt() {
        return pickedUpAt;
    }

    public void setPickedUpAt(String pickedUpAt) {
        this.pickedUpAt = pickedUpAt;
    }

    public String getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(String deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getDriverPhone() {
        return driverPhone;
    }

    public void setDriverPhone(String driverPhone) {
        this.driverPhone = driverPhone;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }

    public double getOrderTotal() {
        return orderTotal;
    }

    public void setOrderTotal(double orderTotal) {
        this.orderTotal = orderTotal;
    }

    @Override
    public String toString() {
        return String.format("Delivery #%d for Order #%d [%s]", id, orderId, status);
    }
}
