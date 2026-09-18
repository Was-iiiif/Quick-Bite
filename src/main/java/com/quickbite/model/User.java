package com.quickbite.model;

import javafx.stage.Stage;

/**
 * Abstract base class representing a system user.
 * Demonstrates OOP Abstraction, Encapsulation, and Polymorphism.
 */
public abstract class User {
    protected int id;
    protected String name;
    protected String email;
    protected String password;
    protected String phone;
    protected String address;
    protected String role;
    protected String createdAt;

    public User() {
    }

    public User(int id, String name, String email, String password, String phone, String address, String role, String createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.address = address;
        this.role = role;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Polymorphic method to launch the role-specific dashboard.
     * Overridden by Customer, RestaurantAdmin, and DeliveryStaff.
     * @param stage Main JavaFX stage
     */
    public abstract void showDashboard(Stage stage);

    @Override
    public String toString() {
        return String.format("%s (%s, ID: %d)", name, role, id);
    }
}
