package com.quickbite.util;

import com.quickbite.model.User;

/**
 * Manages the current session state and logged-in user in the desktop application.
 */
public class SessionContext {
    private static User currentUser;

    public static synchronized void setCurrentUser(User user) {
        currentUser = user;
    }

    public static synchronized User getCurrentUser() {
        return currentUser;
    }

    public static synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    public static synchronized void logout() {
        currentUser = null;
    }
}
