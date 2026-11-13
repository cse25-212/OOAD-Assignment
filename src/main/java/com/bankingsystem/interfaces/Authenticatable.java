package com.bankingsystem.interfaces;

/**
 * Contract for entities capable of authenticating sessions in the Banking System.
 */
public interface Authenticatable {
    boolean login();
    void logout();
    boolean validateCredentials();
}
