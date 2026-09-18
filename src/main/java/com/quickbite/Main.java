package com.quickbite;

import com.quickbite.config.DatabaseConfig;
import com.quickbite.concurrency.OrderProcessingSimulator;
import com.quickbite.controller.LoginView;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main application entry point for QuickBite Food Ordering & Delivery Management System.
 * Initializes SQLite database persistence, seeds data, launches the JavaFX GUI,
 * and ensures graceful shutdown of multithreaded worker pools on exit.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        System.out.println("Starting QuickBite Food Ordering & Delivery Management System...");

        // 1. Initialize SQLite database schema and seed initial data
        DatabaseConfig.initializeDatabase();

        // 2. Ensure background thread pools are gracefully terminated when application window is closed
        primaryStage.setOnCloseRequest(event -> {
            System.out.println("Closing QuickBite. Shutting down background thread pools...");
            OrderProcessingSimulator.getInstance().shutdown();
            System.exit(0);
        });

        // 3. Show primary Login View
        new LoginView().show(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
