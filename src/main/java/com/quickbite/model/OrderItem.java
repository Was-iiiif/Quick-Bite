package com.quickbite.model;

/**
 * Line item inside an Order.
 */
public class OrderItem {
    private int id;
    private int orderId;
    private int foodId;
    private String foodName;
    private int quantity;
    private double unitPrice;

    public OrderItem() {
    }

    public OrderItem(int id, int orderId, int foodId, String foodName, int quantity, double unitPrice) {
        this.id = id;
        this.orderId = orderId;
        this.foodId = foodId;
        this.foodName = foodName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
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

    public int getFoodId() {
        return foodId;
    }

    public void setFoodId(int foodId) {
        this.foodId = foodId;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public double getSubtotal() {
        return quantity * unitPrice;
    }

    @Override
    public String toString() {
        return String.format("%s x%d @ $%.2f = $%.2f", foodName, quantity, unitPrice, getSubtotal());
    }
}
