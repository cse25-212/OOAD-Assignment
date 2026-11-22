package com.bankingsystem.model;

import com.bankingsystem.interfaces.Authenticatable;

/**
 * Handles customer session state and credential verification.
 */
public class Authentication implements Authenticatable {
    private String username;
    private String password;
    private boolean isLoggedIn;

    public Authentication(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank.");
        }
        if (password == null || password.length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters.");
        }
        this.username = username;
        this.password = password;
        this.isLoggedIn = false;
    }

    @Override
    public boolean login() {
        if (validateCredentials()) {
            this.isLoggedIn = true;
            return true;
        }
        return false;
    }

    /**
     * Overloaded login accepting explicit credentials.
     */
    public boolean login(String enteredUsername, String enteredPassword) {
        if (this.username.equals(enteredUsername) && this.password.equals(enteredPassword)) {
            this.isLoggedIn = true;
            return true;
        }
        return false;
    }

    @Override
    public void logout() {
        this.isLoggedIn = false;
    }

    @Override
    public boolean validateCredentials() {
        return username != null && !username.isBlank()
                && password != null && password.length() >= 4;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public String getUsername() {
        return username;
    }
}
