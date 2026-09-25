package com.quickbite.model;

/**
 * Restaurant entity model.
 */
public class Restaurant {
    private int id;
    private String name;
    private String description;
    private String address;
    private String phone;
    private double rating;
    private String imageUrl;
    private int ownerAdminId;

    public Restaurant() {
    }

    public Restaurant(int id, String name, String description, String address, String phone, double rating, String imageUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.address = address;
        this.phone = phone;
        this.rating = rating;
        this.imageUrl = imageUrl;
    }

    public Restaurant(int id, String name, String description, String address, String phone, double rating, String imageUrl, int ownerAdminId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.address = address;
        this.phone = phone;
        this.rating = rating;
        this.imageUrl = imageUrl;
        this.ownerAdminId = ownerAdminId;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getOwnerAdminId() {
        return ownerAdminId;
    }

    public void setOwnerAdminId(int ownerAdminId) {
        this.ownerAdminId = ownerAdminId;
    }

    @Override
    public String toString() {
        return name + " (" + rating + " ★)";
    }
}
