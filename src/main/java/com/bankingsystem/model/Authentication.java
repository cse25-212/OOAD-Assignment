package com.bankingsystem.model;

import com.bankingsystem.interfaces.Authenticatable;
import com.bankingsystem.security.PasswordHasher;

/**
 * Handles customer session state and credential verification.
 */
public class Authentication implements Authenticatable {
    private final String username;
    private final String passwordHash;
    private boolean isLoggedIn;

    public Authentication(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank.");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
        this.username = username;
        this.passwordHash = PasswordHasher.hash(password);
        this.isLoggedIn = false;
    }

    private Authentication(String username, String passwordHash, boolean fromHash) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank.");
        }
        if (!fromHash || !PasswordHasher.isEncodedHash(passwordHash)) {
            throw new IllegalArgumentException("A valid password hash is required.");
        }
        this.username = username;
        this.passwordHash = passwordHash;
        this.isLoggedIn = false;
    }

    public static Authentication fromPasswordHash(String username, String passwordHash) {
        return new Authentication(username, passwordHash, true);
    }

    @Override
    public boolean login() {
        return isLoggedIn;
    }

    /**
     * Overloaded login accepting explicit credentials.
     */
    public boolean login(String enteredUsername, String enteredPassword) {
        if (this.username.equals(enteredUsername) && PasswordHasher.verify(enteredPassword, passwordHash)) {
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
                && PasswordHasher.isEncodedHash(passwordHash);
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
