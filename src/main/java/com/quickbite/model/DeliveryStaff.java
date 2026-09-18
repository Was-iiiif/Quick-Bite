package com.quickbite.model;

import com.quickbite.controller.DeliveryDashboardView;
import javafx.stage.Stage;

/**
 * Concrete class representing Delivery Personnel.
 * Inherits from User and provides polymorphic showDashboard().
 */
public class DeliveryStaff extends User {
    private boolean available;
    private String vehicleType;

    public DeliveryStaff() {
        this.role = "DELIVERY_STAFF";
        this.available = true;
        this.vehicleType = "Motorcycle";
    }

    public DeliveryStaff(int id, String name, String email, String password, String phone, String address, String createdAt, boolean available, String vehicleType) {
        super(id, name, email, password, phone, address, "DELIVERY_STAFF", createdAt);
        this.available = available;
        this.vehicleType = vehicleType;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    @Override
    public void showDashboard(Stage stage) {
        new DeliveryDashboardView(this).show(stage);
    }
}
