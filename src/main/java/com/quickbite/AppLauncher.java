package com.quickbite;

/**
 * Standard JavaFX Application Launcher.
 * This class does NOT extend javafx.application.Application, which circumvents
 * the Java 11+ launcher check ("Error: JavaFX runtime components are missing")
 * when executing JavaFX applications from the classpath.
 */
public class AppLauncher {
    public static void main(String[] args) {
        Main.main(args);
    }
}
