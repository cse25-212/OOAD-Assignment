package com.bankingsystem.interfaces;

/**
 * Contract for accounts supporting cash/fund withdrawals.
 */
public interface Withdrawable {
    boolean withdraw(double amount);
}
