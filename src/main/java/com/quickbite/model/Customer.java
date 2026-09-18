package com.quickbite.model;

import com.quickbite.controller.CustomerDashboardView;
import javafx.stage.Stage;

/**
 * Concrete class representing a Customer user.
 * Inherits from User and provides polymorphic showDashboard().
 */
public class Customer extends User {

    public Customer() {
        this.role = "CUSTOMER";
    }

    public Customer(int id, String name, String email, String password, String phone, String address, String createdAt) {
        super(id, name, email, password, phone, address, "CUSTOMER", createdAt);
    }

    @Override
    public void showDashboard(Stage stage) {
        new CustomerDashboardView(this).show(stage);
    }
}
