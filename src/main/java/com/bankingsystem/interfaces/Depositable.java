package com.bankingsystem.interfaces;

/**
 * Contract for accounts that accept monetary deposits (demonstrating method overloading).
 */
public interface Depositable {
    void deposit(double amount);
    void deposit(double amount, String reference);
}
