package com.quickbite.model;

/**
 * Represents an item in a customer's shopping cart.
 */
public class CartItem {
    private FoodItem foodItem;
    private int quantity;

    public CartItem(FoodItem foodItem, int quantity) {
        this.foodItem = foodItem;
        this.quantity = quantity;
    }

    public FoodItem getFoodItem() {
        return foodItem;
    }

    public void setFoodItem(FoodItem foodItem) {
        this.foodItem = foodItem;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void incrementQuantity() {
        this.quantity++;
    }

    public void decrementQuantity() {
        if (this.quantity > 1) {
            this.quantity--;
        }
    }

    public double getUnitPrice() {
        return foodItem != null ? foodItem.getPrice() : 0.0;
    }

    public double getSubtotal() {
        return getUnitPrice() * quantity;
    }

    @Override
    public String toString() {
        return String.format("%s x%d (BDT %.2f)", foodItem.getName(), quantity, getSubtotal());
    }
}
