package com.quickbite.model;

import com.quickbite.controller.RestaurantDashboardView;
import javafx.stage.Stage;

/**
 * Concrete class representing a Restaurant Manager / Admin.
 * Inherits from User and provides polymorphic showDashboard().
 */
public class RestaurantAdmin extends User {
    private int restaurantId;

    public RestaurantAdmin() {
        this.role = "RESTAURANT_ADMIN";
    }

    public RestaurantAdmin(int id, String name, String email, String password, String phone, String address, String createdAt, int restaurantId) {
        super(id, name, email, password, phone, address, "RESTAURANT_ADMIN", createdAt);
        this.restaurantId = restaurantId;
    }

    public int getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(int restaurantId) {
        this.restaurantId = restaurantId;
    }

    @Override
    public void showDashboard(Stage stage) {
        new RestaurantDashboardView(this).show(stage);
    }
}
